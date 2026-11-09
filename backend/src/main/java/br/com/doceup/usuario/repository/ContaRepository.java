package br.com.doceup.usuario.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ContaRepository {
    private final JdbcClient jdbc;

    public ContaRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UUID> buscarNegocio(String emissor, String identificador) {
        return jdbc.sql("""
                SELECT negocio_id FROM usuario
                WHERE emissor = :emissor AND identificador = :identificador
                """).param("emissor", emissor).param("identificador", identificador)
                .query(UUID.class).optional();
    }

    public void criarNegocio(UUID negocio) {
        jdbc.sql("INSERT INTO negocio (id) VALUES (:id)").param("id", negocio).update();
    }

    public void bloquearCadastro(String emissor, String identificador) {
        jdbc.sql("SELECT pg_advisory_xact_lock(hashtextextended(:identidade, 0))")
                .param("identidade", emissor + "\n" + identificador)
                .query((rs, linha) -> Boolean.TRUE).single();
    }

    public void criarUsuario(String emissor, String identificador, UUID negocio) {
        jdbc.sql("""
                INSERT INTO usuario (id, emissor, identificador, negocio_id)
                VALUES (:id, :emissor, :identificador, :negocio)
                """).param("id", UUID.randomUUID()).param("emissor", emissor)
                .param("identificador", identificador).param("negocio", negocio).update();
    }
}
