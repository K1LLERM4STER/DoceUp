package br.com.doceup.estoque.repository;

import br.com.doceup.estoque.entity.Insumo;
import br.com.doceup.estoque.entity.TipoInsumo;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface InsumoRepository extends JpaRepository<Insumo, UUID> {
    Optional<Insumo> findByIdAndNegocioId(UUID id, UUID negocioId);

    Optional<Insumo> findByNegocioIdAndTipo(UUID negocioId, TipoInsumo tipo);

    Page<Insumo> findByNegocioIdAndNomeContainingIgnoreCase(UUID negocioId, String nome, Pageable page);

    boolean existsByNegocioIdAndNomeIgnoreCase(UUID negocioId, String nome);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Insumo i where i.id = :id and i.negocioId = :negocioId")
    Optional<Insumo> findForUpdateByIdAndNegocioId(@Param("id") UUID id, @Param("negocioId") UUID negocioId);

    @Query("select i.id from Insumo i where i.negocioId = :negocioId and i.tipo = :tipo")
    Optional<UUID> buscarIdPorNegocioETipo(@Param("negocioId") UUID negocioId, @Param("tipo") TipoInsumo tipo);
}
