package br.com.doceup.venda.service;

import br.com.doceup.estoque.entity.TipoMovimento;
import br.com.doceup.estoque.service.MovimentoService;
import br.com.doceup.exception.RegraException;
import br.com.doceup.produto.entity.Produto;
import br.com.doceup.produto.repository.ProdutoRepository;
import br.com.doceup.venda.dto.NovaVenda;
import br.com.doceup.venda.dto.PaginaVendas;
import br.com.doceup.venda.dto.VendaResposta;
import br.com.doceup.venda.entity.ItemVenda;
import br.com.doceup.venda.entity.Recebimento;
import br.com.doceup.venda.entity.Venda;
import br.com.doceup.venda.mapper.VendaMapper;
import br.com.doceup.venda.repository.RecebimentoRepository;
import br.com.doceup.venda.repository.VendaRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VendaService {
    private static final BigDecimal LIMITE_VALOR = new BigDecimal("99999999999999999.99");

    private final VendaRepository vendas;
    private final RecebimentoRepository recebimentos;
    private final ProdutoRepository produtos;
    private final MovimentoService movimentos;
    private final Clock clock;

    public VendaService(
            VendaRepository vendas, RecebimentoRepository recebimentos, ProdutoRepository produtos,
            MovimentoService movimentos, Clock clock
    ) {
        this.vendas = vendas;
        this.recebimentos = recebimentos;
        this.produtos = produtos;
        this.movimentos = movimentos;
        this.clock = clock;
    }

    @Transactional
    public VendaResposta registrar(UUID negocioId, NovaVenda nova) {
        var ids = nova.itens().stream().map(NovaVenda.Item::produtoId).distinct().sorted().toList();
        if (ids.size() != nova.itens().size()) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "PRODUTO_REPETIDO",
                    "Informe uma quantidade por produto na venda.");
        }

        Map<UUID, Produto> itensBloqueados = new HashMap<>();
        for (UUID id : ids) {
            var produto = produtos.findForUpdateByIdAndNegocioId(id, negocioId)
                    .orElseThrow(() -> new RegraException(HttpStatus.NOT_FOUND, "PRODUTO_NAO_ENCONTRADO",
                            "Produto não encontrado."));
            itensBloqueados.put(id, produto);
        }

        var itens = nova.itens().stream().map(item -> {
            var produto = itensBloqueados.get(item.produtoId());
            return new ItemVenda(produto.getId(), produto.getNome(), item.quantidade(), produto.getPrecoVenda());
        }).toList();
        BigDecimal sugerido = itens.stream().map(ItemVenda::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = nova.valorTotal() == null ? sugerido : nova.valorTotal();
        if (total.signum() < 0 || total.compareTo(LIMITE_VALOR) > 0 || total.scale() > 2) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "VALOR_TOTAL_INVALIDO",
                    "Confira o valor total da venda.");
        }

        for (ItemVenda item : itens) {
            itensBloqueados.get(item.getProdutoId()).vender(item.getQuantidade());
        }
        var venda = registrarVenda(negocioId, total, nova.observacao(), itens);
        for (ItemVenda item : itens) {
            movimentos.registrarProduto(negocioId, item.getProdutoId(), TipoMovimento.VENDA,
                    BigDecimal.valueOf(-item.getQuantidade()), venda.getId(), null);
        }
        Recebimento recebimento = nova.pagamentoPendente() ? null : receberNaVenda(venda);
        return VendaMapper.paraResposta(venda, recebimento);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UUID registrarEntrega(UUID negocioId, BigDecimal total, String observacao, List<ItemVenda> itens) {
        Venda venda = registrarVenda(negocioId, total, observacao, itens);
        receberNaVenda(venda);
        return venda.getId();
    }

    private Venda registrarVenda(UUID negocioId, BigDecimal total, String observacao, List<ItemVenda> itens) {
        return vendas.saveAndFlush(new Venda(negocioId, clock.instant(),
                total.setScale(2, RoundingMode.UNNECESSARY), observacao, itens));
    }

    private Recebimento receberNaVenda(Venda venda) {
        return recebimentos.save(new Recebimento(venda, venda.getVendidaEm()));
    }

    @Transactional
    public VendaResposta receber(UUID negocioId, UUID vendaId) {
        var venda = vendas.findForUpdateByIdAndNegocioId(vendaId, negocioId)
                .orElseThrow(VendaService::naoEncontrada);
        var recebimento = recebimentos.findByVendaIdAndNegocioId(vendaId, negocioId)
                .orElseGet(() -> recebimentos.save(new Recebimento(venda, clock.instant())));
        return VendaMapper.paraResposta(venda, recebimento);
    }

    @Transactional(readOnly = true)
    public VendaResposta buscar(UUID negocioId, UUID vendaId) {
        var venda = vendas.findByIdAndNegocioId(vendaId, negocioId).orElseThrow(VendaService::naoEncontrada);
        var recebimento = recebimentos.findByVendaIdAndNegocioId(vendaId, negocioId).orElse(null);
        return VendaMapper.paraResposta(venda, recebimento);
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public PaginaVendas listar(UUID negocioId, int pagina, int tamanho) {
        if (pagina < 0 || pagina > 10_000 || tamanho < 1 || tamanho > 50) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "PAGINA_INVALIDA", "Confira a página solicitada.");
        }
        var vendasPaginadas = vendas.findByNegocioIdOrderByVendidaEmDescIdDesc(negocioId,
                PageRequest.of(pagina, tamanho));
        if (vendasPaginadas.isEmpty()) {
            return new PaginaVendas(List.of(), pagina, tamanho,
                    vendasPaginadas.getTotalElements(), vendasPaginadas.getTotalPages());
        }

        var ids = vendasPaginadas.stream().map(Venda::getId).toList();
        vendas.carregarItens(negocioId, ids);
        var recebimentosPorVenda = recebimentos.findByNegocioIdAndVendaIdIn(negocioId, ids).stream()
                .collect(Collectors.toMap(Recebimento::getVendaId, Function.identity()));
        var respostas = vendasPaginadas.stream()
                .map(venda -> VendaMapper.paraResposta(venda, recebimentosPorVenda.get(venda.getId())))
                .toList();
        return new PaginaVendas(respostas, pagina, tamanho,
                vendasPaginadas.getTotalElements(), vendasPaginadas.getTotalPages());
    }

    private static RegraException naoEncontrada() {
        return new RegraException(HttpStatus.NOT_FOUND, "VENDA_NAO_ENCONTRADA", "Venda não encontrada.");
    }
}
