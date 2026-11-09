package br.com.doceup.estoque.service;

import br.com.doceup.estoque.entity.MovimentoEstoque;
import br.com.doceup.estoque.entity.TipoMovimento;
import br.com.doceup.estoque.repository.MovimentoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class MovimentoService {
    private final MovimentoRepository movimentos;
    private final Clock relogio;

    public MovimentoService(MovimentoRepository movimentos, Clock relogio) {
        this.movimentos = movimentos;
        this.relogio = relogio;
    }

    public void registrarProduto(UUID negocioId, UUID produtoId, TipoMovimento tipo, BigDecimal quantidade,
                                 UUID operacaoId, String motivo) {
        movimentos.save(new MovimentoEstoque(negocioId, null, produtoId, tipo, quantidade, operacaoId,
                motivo, relogio.instant()));
    }

    public void registrarInsumo(UUID negocioId, UUID insumoId, TipoMovimento tipo, BigDecimal quantidade,
                                UUID operacaoId, String motivo, Instant data) {
        movimentos.save(new MovimentoEstoque(negocioId, insumoId, null, tipo, quantidade, operacaoId,
                motivo, data));
    }
}
