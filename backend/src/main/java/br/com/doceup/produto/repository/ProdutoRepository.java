package br.com.doceup.produto.repository;

import br.com.doceup.produto.entity.Produto;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface ProdutoRepository extends JpaRepository<Produto, UUID> {
    Optional<Produto> findByIdAndNegocioId(UUID id, UUID negocioId);

    Page<Produto> findByNegocioIdAndAtivoTrueAndNomeContainingIgnoreCase(
            UUID negocioId, String nome, Pageable pagina);

    boolean existsByNegocioIdAndNomeIgnoreCase(UUID negocioId, String nome);

    boolean existsByNegocioIdAndNomeIgnoreCaseAndIdNot(UUID negocioId, String nome, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Produto p where p.id = :id and p.negocioId = :negocioId")
    Optional<Produto> findForUpdateByIdAndNegocioId(@Param("id") UUID id, @Param("negocioId") UUID negocioId);
}
