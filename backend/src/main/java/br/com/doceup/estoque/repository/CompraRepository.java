package br.com.doceup.estoque.repository;

import br.com.doceup.estoque.entity.CompraInsumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface CompraRepository extends JpaRepository<CompraInsumo, UUID> {
    @Query("""
            select coalesce(sum(c.valorTotal), 0) from CompraInsumo c
            where c.negocioId = :negocioId and c.data >= :inicio and c.data < :fim
            """)
    BigDecimal sumByNegocioIdAndPeriodo(@Param("negocioId") UUID negocioId, @Param("inicio") Instant inicio,
                                      @Param("fim") Instant fim);

    Optional<CompraInsumo> findFirstByNegocioIdAndInsumoIdAndValorTotalGreaterThanOrderByDataDescOrdemDesc(
            UUID negocioId, UUID insumoId, BigDecimal valor);
}
