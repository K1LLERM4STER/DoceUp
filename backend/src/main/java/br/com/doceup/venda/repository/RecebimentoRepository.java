package br.com.doceup.venda.repository;

import br.com.doceup.venda.entity.Recebimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecebimentoRepository extends JpaRepository<Recebimento, UUID> {
    Optional<Recebimento> findByVendaIdAndNegocioId(UUID vendaId, UUID negocioId);

    List<Recebimento> findByNegocioIdAndVendaIdIn(UUID negocioId, Collection<UUID> ids);

    @Query("""
            select sum(r.valor) from Recebimento r
            where r.negocioId = :negocioId and r.recebidoEm >= :inicio and r.recebidoEm < :fim
            """)
    BigDecimal somarNoPeriodo(
            @Param("negocioId") UUID negocioId,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim
    );
}
