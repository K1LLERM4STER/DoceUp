package br.com.doceup.estoque.repository;

import br.com.doceup.estoque.entity.MovimentoEstoque;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface MovimentoRepository extends JpaRepository<MovimentoEstoque, UUID> {}
