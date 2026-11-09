package br.com.doceup.producao.repository;

import br.com.doceup.producao.entity.Producao;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProducaoRepository extends JpaRepository<Producao, UUID> {
    Optional<Producao> findFirstByNegocioIdAndProdutoIdOrderByDataDescOrdemDesc(UUID negocioId, UUID produtoId);
}
