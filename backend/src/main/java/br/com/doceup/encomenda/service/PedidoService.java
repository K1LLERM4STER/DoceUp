package br.com.doceup.encomenda.service;

import br.com.doceup.encomenda.dto.NovoPedido;
import br.com.doceup.encomenda.dto.PaginaPedidos;
import br.com.doceup.encomenda.dto.PedidoResposta;
import br.com.doceup.encomenda.entity.ItemPedido;
import br.com.doceup.encomenda.entity.Pedido;
import br.com.doceup.encomenda.entity.StatusPedido;
import br.com.doceup.encomenda.mapper.PedidoMapper;
import br.com.doceup.encomenda.repository.PedidoRepository;
import br.com.doceup.estoque.entity.TipoMovimento;
import br.com.doceup.estoque.service.MovimentoService;
import br.com.doceup.exception.RegraException;
import br.com.doceup.produto.entity.Produto;
import br.com.doceup.produto.repository.ProdutoRepository;
import br.com.doceup.produto.service.ProdutoService;
import br.com.doceup.shared.HorarioDoNegocio;
import br.com.doceup.venda.entity.ItemVenda;
import br.com.doceup.venda.service.VendaService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PedidoService {
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("99999999999999999.99");

    private final PedidoRepository pedidos;
    private final ProdutoRepository produtos;
    private final MovimentoService movimentos;
    private final VendaService vendas;
    private final Clock clock;

    public PedidoService(PedidoRepository pedidos, ProdutoRepository produtos, MovimentoService movimentos,
                         VendaService vendas, Clock clock) {
        this.pedidos = pedidos;
        this.produtos = produtos;
        this.movimentos = movimentos;
        this.vendas = vendas;
        this.clock = clock;
    }

    @Transactional
    public PedidoResposta reservar(UUID negocioId, NovoPedido novo) {
        Instant agora = clock.instant();
        if (novo.dataEntrega().isBefore(HorarioDoNegocio.dataDe(agora)) || novo.dataEntrega().getYear() > 9999) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "DATA_ENTREGA_INVALIDA",
                    "Informe uma data de entrega a partir de hoje.");
        }
        var ids = novo.itens().stream().map(NovoPedido.Item::produtoId).distinct().toList();
        if (ids.size() != novo.itens().size()) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "PRODUTO_REPETIDO",
                    "Informe uma quantidade por produto na encomenda.");
        }
        Map<UUID, Produto> bloqueados = bloquearProdutos(negocioId, ids);
        List<ItemPedido> itens = novo.itens().stream()
                .map(item -> new ItemPedido(negocioId, bloqueados.get(item.produtoId()), item.quantidade()))
                .toList();
        BigDecimal total = itens.stream().map(ItemPedido::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.compareTo(LIMITE_VALOR) > 0) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "VALOR_TOTAL_INVALIDO",
                    "O total da encomenda ultrapassa o valor permitido.");
        }
        for (ItemPedido item : itens) {
            bloqueados.get(item.getProdutoId()).reservar(item.getQuantidade());
        }
        var pedido = pedidos.saveAndFlush(new Pedido(negocioId, novo.cliente().strip(), novo.dataEntrega(),
                agora, novo.observacao(), total, itens));
        registrarMovimentos(negocioId, pedido, TipoMovimento.RESERVA, -1);
        return PedidoMapper.paraResposta(pedido);
    }

    @Transactional
    public PedidoResposta concluir(UUID negocioId, UUID pedidoId) {
        Pedido pedido = bloquearPedido(negocioId, pedidoId);
        if (pedido.getStatus() == StatusPedido.CONCLUIDA) {
            return PedidoMapper.paraResposta(pedido);
        }
        exigirAberto(pedido);

        var itens = pedido.getItens();
        Map<UUID, Produto> bloqueados = bloquearProdutos(negocioId, itens.stream().map(ItemPedido::getProdutoId).toList());
        for (ItemPedido item : itens) {
            bloqueados.get(item.getProdutoId()).entregarReserva(item.getQuantidade());
        }
        var itensVenda = itens.stream().map(item -> new ItemVenda(item.getProdutoId(), item.getNome(),
                item.getQuantidade(), item.getPrecoUnitario())).toList();
        UUID vendaId = vendas.registrarEntrega(negocioId, pedido.getValorTotal(), pedido.getObservacao(), itensVenda);
        pedido.concluir(vendaId);
        registrarMovimentos(negocioId, pedido, TipoMovimento.ENTREGA, -1);
        return PedidoMapper.paraResposta(pedido);
    }

    @Transactional
    public PedidoResposta cancelar(UUID negocioId, UUID pedidoId) {
        Pedido pedido = bloquearPedido(negocioId, pedidoId);
        if (pedido.getStatus() == StatusPedido.CANCELADA) {
            return PedidoMapper.paraResposta(pedido);
        }
        exigirAberto(pedido);

        var itens = pedido.getItens();
        Map<UUID, Produto> bloqueados = bloquearProdutos(negocioId, itens.stream().map(ItemPedido::getProdutoId).toList());
        for (ItemPedido item : itens) {
            bloqueados.get(item.getProdutoId()).liberar(item.getQuantidade());
        }
        pedido.cancelar();
        registrarMovimentos(negocioId, pedido, TipoMovimento.LIBERACAO, 1);
        return PedidoMapper.paraResposta(pedido);
    }

    @Transactional(readOnly = true)
    public PedidoResposta buscar(UUID negocioId, UUID pedidoId) {
        return pedidos.findByIdAndNegocioId(pedidoId, negocioId).map(PedidoMapper::paraResposta)
                .orElseThrow(PedidoService::naoEncontrado);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaPedidos listar(UUID negocioId, StatusPedido status, LocalDate dataEntrega, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 10_000 || tamanho < 1 || tamanho > 50) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "PAGINA_INVALIDA", "Confira a página solicitada.");
        }
        Specification<Pedido> filtro = (raiz, consulta, builder) -> builder.equal(raiz.get("negocioId"), negocioId);
        if (status != null) {
            filtro = filtro.and((raiz, consulta, builder) -> builder.equal(raiz.get("status"), status));
        }
        if (dataEntrega != null) {
            filtro = filtro.and((raiz, consulta, builder) -> builder.equal(raiz.get("dataEntrega"), dataEntrega));
        }
        var ordem = Sort.by(Sort.Order.asc("dataEntrega"), Sort.Order.desc("criadoEm"), Sort.Order.desc("id"));
        var resultados = pedidos.findAll(filtro, PageRequest.of(pagina, tamanho, ordem));
        if (!resultados.isEmpty()) {
            pedidos.carregarItens(negocioId, resultados.stream().map(Pedido::getId).toList());
        }
        return new PaginaPedidos(resultados.stream().map(PedidoMapper::paraResposta).toList(),
                pagina, tamanho, resultados.getTotalElements(), resultados.getTotalPages());
    }

    private Pedido bloquearPedido(UUID negocioId, UUID id) {
        return pedidos.findForUpdateByIdAndNegocioId(id, negocioId).orElseThrow(PedidoService::naoEncontrado);
    }

    private Map<UUID, Produto> bloquearProdutos(UUID negocioId, List<UUID> ids) {
        Map<UUID, Produto> bloqueados = new HashMap<>();
        for (UUID id : ids.stream().sorted().toList()) {
            bloqueados.put(id, produtos.findForUpdateByIdAndNegocioId(id, negocioId)
                    .orElseThrow(ProdutoService::naoEncontrado));
        }
        return bloqueados;
    }

    private void registrarMovimentos(UUID negocioId, Pedido pedido, TipoMovimento tipo, int sinal) {
        for (ItemPedido item : pedido.getItens()) {
            movimentos.registrarProduto(negocioId, item.getProdutoId(), tipo,
                    BigDecimal.valueOf((long) sinal * item.getQuantidade()), pedido.getId(), null);
        }
    }

    private static void exigirAberto(Pedido pedido) {
        if (pedido.getStatus() != StatusPedido.ABERTA) {
            throw new RegraException(HttpStatus.CONFLICT, "PEDIDO_ENCERRADO", "Esta encomenda já foi encerrada.");
        }
    }

    private static RegraException naoEncontrado() {
        return new RegraException(HttpStatus.NOT_FOUND, "PEDIDO_NAO_ENCONTRADO", "Encomenda não encontrada.");
    }
}
