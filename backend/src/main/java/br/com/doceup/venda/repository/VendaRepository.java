package br.com.doceup.venda.repository;

import br.com.doceup.venda.entity.Venda;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VendaRepository extends JpaRepository<Venda, UUID> {
    Optional<Venda> findByIdAndNegocioId(UUID id, UUID negocioId);

    Page<Venda> findByNegocioIdOrderByVendidaEmDescIdDesc(UUID negocioId, Pageable pagina);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Venda v where v.id = :id and v.negocioId = :negocioId")
    Optional<Venda> findForUpdateByIdAndNegocioId(@Param("id") UUID id, @Param("negocioId") UUID negocioId);

    @Query("select distinct v from Venda v join fetch v.itens where v.negocioId = :negocioId and v.id in :ids")
    List<Venda> carregarItens(@Param("negocioId") UUID negocioId, @Param("ids") Collection<UUID> ids);

    @Query("""
            select count(v) from Venda v where v.negocioId = :negocioId and not exists
                (select r.vendaId from Recebimento r where r.vendaId = v.id and r.negocioId = :negocioId)
            """)
    long contarPendentes(@Param("negocioId") UUID negocioId);
}
