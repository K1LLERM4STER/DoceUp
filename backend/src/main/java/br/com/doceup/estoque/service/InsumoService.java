package br.com.doceup.estoque.service;

import br.com.doceup.estoque.dto.InsumoDtos;
import br.com.doceup.estoque.dto.PaginaEstoque;
import br.com.doceup.estoque.entity.*;
import br.com.doceup.estoque.mapper.InsumoMapper;
import br.com.doceup.estoque.repository.CompraRepository;
import br.com.doceup.estoque.repository.InsumoRepository;
import br.com.doceup.exception.RegraException;
import br.com.doceup.shared.HorarioDoNegocio;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InsumoService {
    private static final String NOME_EMBALAGEM = "Embalagens";
    private final InsumoRepository insumos;
    private final CompraRepository compras;
    private final MovimentoService movimentos;
    private final Clock relogio;

    public InsumoService(InsumoRepository insumos, CompraRepository compras, MovimentoService movimentos, Clock relogio) {
        this.insumos = insumos;
        this.compras = compras;
        this.movimentos = movimentos;
        this.relogio = relogio;
    }

    @Transactional
    public InsumoDtos.Insumo criar(UUID negocioId, InsumoDtos.Criar entrada) {
        String nome = entrada.nome().strip();
        if (entrada.tipo() == TipoInsumo.EMBALAGEM) {
            return InsumoMapper.resposta(insumos.findByNegocioIdAndTipo(negocioId, TipoInsumo.EMBALAGEM)
                    .orElseThrow(InsumoService::naoEncontrado));
        }
        if (NOME_EMBALAGEM.equalsIgnoreCase(nome) || insumos.existsByNegocioIdAndNomeIgnoreCase(negocioId, nome)) {
            throw new RegraException(HttpStatus.CONFLICT, "INSUMO_DUPLICADO", "Já existe um insumo com esse nome.");
        }
        return InsumoMapper.resposta(insumos.save(new Insumo(negocioId, nome, entrada.unidade(), TipoInsumo.INGREDIENTE)));
    }

    @Transactional(readOnly = true)
    public PaginaEstoque<InsumoDtos.Insumo> listar(UUID negocioId, String busca, Pageable pagina) {
        var ordenada = PageRequest.of(pagina.getPageNumber(), pagina.getPageSize(), Sort.by("nome", "id"));
        return PaginaEstoque.de(insumos.findByNegocioIdAndNomeContainingIgnoreCase(negocioId, busca.strip(), ordenada)
                .map(InsumoMapper::resposta));
    }

    @Transactional(readOnly = true)
    public InsumoDtos.Insumo obter(UUID negocioId, UUID id) {
        return InsumoMapper.resposta(insumos.findByIdAndNegocioId(id, negocioId).orElseThrow(InsumoService::naoEncontrado));
    }

    @Transactional
    public InsumoDtos.Insumo comprar(UUID negocioId, UUID id, InsumoDtos.Compra entrada) {
        Insumo insumo = insumos.findForUpdateByIdAndNegocioId(id, negocioId).orElseThrow(InsumoService::naoEncontrado);
        Instant agora = relogio.instant();
        LocalDate hoje = HorarioDoNegocio.dataDe(agora);
        LocalDate data = entrada.data() == null ? hoje : entrada.data();
        if (data.isAfter(hoje)) {
            throw new RegraException(HttpStatus.BAD_REQUEST, "COMPRA_DATA_INVALIDA", "Informe uma data de compra até hoje.");
        }
        Instant instante = data.equals(hoje) ? agora : HorarioDoNegocio.inicioDe(data);
        var compra = compras.save(new CompraInsumo(negocioId, id, entrada.quantidade(), entrada.valorTotal(), instante));
        boolean gratuita = entrada.valorTotal().signum() == 0;
        BigDecimal custoUnitario = null;
        if (!gratuita && insumo.getTipo() == TipoInsumo.INGREDIENTE) {
            var ultima = compras.findFirstByNegocioIdAndInsumoIdAndValorTotalGreaterThanOrderByDataDescOrdemDesc(
                    negocioId, id, BigDecimal.ZERO).orElseThrow();
            custoUnitario = ultima.getValorTotal().divide(ultima.getQuantidade(), 6, RoundingMode.HALF_UP);
        }
        insumo.adicionar(entrada.quantidade(), custoUnitario);
        movimentos.registrarInsumo(negocioId, id, gratuita ? TipoMovimento.ENTRADA_GRATUITA : TipoMovimento.COMPRA,
                entrada.quantidade(), compra.getId(), null, instante);
        return InsumoMapper.resposta(insumo);
    }

    @Transactional
    public InsumoDtos.Insumo ajustar(UUID negocioId, UUID id, InsumoDtos.Ajuste entrada) {
        Insumo insumo = insumos.findForUpdateByIdAndNegocioId(id, negocioId).orElseThrow(InsumoService::naoEncontrado);
        BigDecimal diferenca = entrada.quantidade().subtract(insumo.getQuantidade());
        insumo.ajustar(entrada.quantidade());
        if (diferenca.signum() != 0) {
            movimentos.registrarInsumo(negocioId, id, TipoMovimento.AJUSTE, diferenca, UUID.randomUUID(),
                    entrada.motivo(), relogio.instant());
        }
        return InsumoMapper.resposta(insumo);
    }

    public static RegraException naoEncontrado() {
        return new RegraException(HttpStatus.NOT_FOUND, "INSUMO_NAO_ENCONTRADO", "Insumo não encontrado.");
    }
}
