package br.com.doceup.produto.repository;

import br.com.doceup.produto.entity.FotoProduto;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FotoRepository extends JpaRepository<FotoProduto, UUID> {
    Optional<FotoProduto> findByProdutoIdAndNegocioId(UUID produtoId, UUID negocioId);
}
