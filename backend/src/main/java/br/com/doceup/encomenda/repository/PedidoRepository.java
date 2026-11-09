package br.com.doceup.encomenda.repository;

import br.com.doceup.encomenda.entity.Pedido;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PedidoRepository extends JpaRepository<Pedido, UUID>, JpaSpecificationExecutor<Pedido> {
    Optional<Pedido> findByIdAndNegocioId(UUID id, UUID negocioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pedido p where p.id = :id and p.negocioId = :negocioId")
    Optional<Pedido> findForUpdateByIdAndNegocioId(@Param("id") UUID id, @Param("negocioId") UUID negocioId);

    @Query("select distinct p from Pedido p left join fetch p.itens where p.negocioId = :negocioId and p.id in :ids")
    List<Pedido> carregarItens(@Param("negocioId") UUID negocioId, @Param("ids") Collection<UUID> ids);
}
