package br.com.doceup.producao.service;

import br.com.doceup.estoque.entity.Insumo;
import br.com.doceup.estoque.entity.TipoInsumo;
import br.com.doceup.estoque.entity.TipoMovimento;
import br.com.doceup.estoque.repository.InsumoRepository;
import br.com.doceup.estoque.service.InsumoService;
import br.com.doceup.estoque.service.MovimentoService;
import br.com.doceup.exception.RegraException;
import br.com.doceup.producao.dto.ProducaoDtos;
import br.com.doceup.producao.entity.ItemProducao;
import br.com.doceup.producao.entity.Producao;
import br.com.doceup.producao.mapper.ProducaoMapper;
import br.com.doceup.producao.repository.ProducaoRepository;
import br.com.doceup.produto.repository.ProdutoRepository;
import br.com.doceup.produto.service.ProdutoService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProducaoService {
    private final ProdutoRepository produtos;
    private final InsumoRepository insumos;
    private final ProducaoRepository producoes;
    private final MovimentoService movimentos;
    private final Clock relogio;

    public ProducaoService(ProdutoRepository produtos, InsumoRepository insumos, ProducaoRepository producoes,
                           MovimentoService movimentos, Clock relogio) {
        this.produtos = produtos;
        this.insumos = insumos;
        this.producoes = producoes;
        this.movimentos = movimentos;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Optional<ProducaoDtos.Base> ultima(UUID negocioId, UUID produtoId) {
        produtos.findByIdAndNegocioId(produtoId, negocioId).orElseThrow(ProdutoService::naoEncontrado).exigirAtivo();
        return producoes.findFirstByNegocioIdAndProdutoIdOrderByDataDescOrdemDesc(negocioId, produtoId)
                .map(ProducaoMapper::base);
    }

    @Transactional(readOnly = true)
    public ProducaoDtos.Resumo previa(UUID negocioId, UUID produtoId, ProducaoDtos.Entrada entrada) {
        produtos.findByIdAndNegocioId(produtoId, negocioId).orElseThrow(ProdutoService::naoEncontrado).exigirAtivo();
        return calcular(negocioId, entrada, false).resumo(null);
    }

    @Transactional
    public ProducaoDtos.Resumo confirmar(UUID negocioId, UUID produtoId, ProducaoDtos.Entrada entrada) {
        var produto = produtos.findForUpdateByIdAndNegocioId(produtoId, negocioId)
                .orElseThrow(ProdutoService::naoEncontrado);
        produto.exigirAtivo();
        var calculo = calcular(negocioId, entrada, true);
        if (!calculo.faltas().isEmpty() && !entrada.aceitarEstoqueNegativo()) {
            throw new RegraException(HttpStatus.CONFLICT, "ESTOQUE_INSUFICIENTE",
                    "Faltam insumos. Ajuste o estoque ou confirme que deseja produzir mesmo assim.",
                    Map.of("faltas", calculo.faltas()));
        }

        Instant agora = relogio.instant();
        var producao = producoes.save(new Producao(negocioId, produtoId, agora, entrada.rendimento(),
                entrada.quantidadeEmbalagem(), entrada.custoEmbalagem(), calculo.custoTotal(),
                calculo.custoUnitario(), calculo.itens()));
        produto.produzir(entrada.rendimento(), calculo.custoUnitario());
        movimentos.registrarProduto(negocioId, produtoId, TipoMovimento.PRODUCAO,
                BigDecimal.valueOf(entrada.rendimento()), producao.getId(), null);
        for (var consumo : calculo.consumos()) {
            consumo.insumo().consumir(consumo.quantidade());
            movimentos.registrarInsumo(negocioId, consumo.insumo().getId(), TipoMovimento.CONSUMO,
                    consumo.quantidade().negate(), producao.getId(), null, agora);
        }
        return calculo.resumo(producao.getId());
    }

    private Calculo calcular(UUID negocioId, ProducaoDtos.Entrada entrada, boolean bloquear) {
        // Ordenação estável para aquisições de bloqueio em produções concorrentes.
        var quantidades = new TreeMap<UUID, BigDecimal>();
        for (var item : entrada.ingredientes()) {
            if (quantidades.putIfAbsent(item.insumoId(), item.quantidade()) != null) {
                throw invalida("Cada ingrediente pode aparecer uma única vez.");
            }
        }
        UUID embalagemId = incluirEmbalagem(negocioId, entrada, quantidades);

        var consumos = new ArrayList<Consumo>();
        var itens = new ArrayList<ItemProducao>();
        var faltas = new ArrayList<ProducaoDtos.Falta>();
        for (var item : quantidades.entrySet()) {
            UUID insumoId = item.getKey();
            Insumo insumo = (bloquear ? insumos.findForUpdateByIdAndNegocioId(insumoId, negocioId)
                    : insumos.findByIdAndNegocioId(insumoId, negocioId)).orElseThrow(InsumoService::naoEncontrado);
            BigDecimal necessario = item.getValue();
            consumos.add(new Consumo(insumo, necessario));
            if (insumo.getTipo() == TipoInsumo.INGREDIENTE) {
                BigDecimal custo = Objects.requireNonNullElse(insumo.getUltimoCustoUnitario(), BigDecimal.ZERO);
                itens.add(new ItemProducao(insumoId, negocioId, necessario, custo));
            } else if (!insumoId.equals(embalagemId)) {
                throw invalida("Informe embalagens no campo Tem embalagem?");
            }

            BigDecimal falta = necessario.subtract(insumo.getQuantidade());
            if (falta.signum() > 0) {
                faltas.add(new ProducaoDtos.Falta(insumoId, insumo.getNome(), insumo.getUnidade(),
                        insumo.getQuantidade(), necessario, falta));
            }
        }
        return new Calculo(entrada.rendimento(), entrada.custoEmbalagem(), consumos, itens, faltas);
    }

    private UUID incluirEmbalagem(UUID negocioId, ProducaoDtos.Entrada entrada, Map<UUID, BigDecimal> quantidades) {
        if (entrada.quantidadeEmbalagem().signum() == 0) {
            if (entrada.custoEmbalagem().signum() != 0) {
                throw invalida("Informe a quantidade de embalagens usadas.");
            }
            return null;
        }
        UUID embalagem = insumos.buscarIdPorNegocioETipo(negocioId, TipoInsumo.EMBALAGEM)
                .orElseThrow(InsumoService::naoEncontrado);
        if (quantidades.putIfAbsent(embalagem, entrada.quantidadeEmbalagem()) != null) {
            throw invalida("Informe embalagens no campo Tem embalagem?");
        }
        return embalagem;
    }

    private static RegraException invalida(String mensagem) {
        return new RegraException(HttpStatus.BAD_REQUEST, "PRODUCAO_INVALIDA", mensagem);
    }

    private record Consumo(Insumo insumo, BigDecimal quantidade) {}

    private record Calculo(int rendimento, BigDecimal custoEmbalagem, List<Consumo> consumos,
                           List<ItemProducao> itens, List<ProducaoDtos.Falta> faltas) {
        private BigDecimal custoIngredientes() {
            return itens.stream().map(ItemProducao::calcularCusto).reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        private BigDecimal custoTotal() {
            return custoIngredientes().add(custoEmbalagem);
        }

        private BigDecimal custoUnitario() {
            return custoTotal().divide(BigDecimal.valueOf(rendimento), 6, RoundingMode.HALF_UP);
        }

        private ProducaoDtos.Resumo resumo(UUID id) {
            return new ProducaoDtos.Resumo(id, rendimento, custoIngredientes(), custoEmbalagem(), custoTotal(),
                    custoUnitario(), faltas);
        }
    }
}
