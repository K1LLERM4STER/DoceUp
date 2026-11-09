package br.com.doceup.shared;

import br.com.doceup.exception.RegraException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@Service
public class Idempotencia {
    private final JdbcClient jdbc;
    private final JsonMapper json;

    public Idempotencia(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    @Transactional
    public <T> T executar(UUID negocioId, UUID chave, String operacao, Object payload,
                          Class<T> tipo, Supplier<T> acao) {
        String assinatura = assinatura(json.writeValueAsString(payload));
        int inserido = jdbc.sql("""
                INSERT INTO operacao_idempotente (negocio_id, chave, operacao, assinatura)
                VALUES (:negocio, :chave, :operacao, :assinatura)
                ON CONFLICT (negocio_id, chave) DO NOTHING
                """).param("negocio", negocioId).param("chave", chave).param("operacao", operacao)
                .param("assinatura", assinatura).update();
        if (inserido == 0) {
            Registro anterior = jdbc.sql("""
                    SELECT operacao, assinatura, resposta FROM operacao_idempotente
                    WHERE negocio_id = :negocio AND chave = :chave
                    """).param("negocio", negocioId).param("chave", chave).query((rs, linha) ->
                            new Registro(rs.getString(1), rs.getString(2), rs.getString(3))).single();
            if (!operacao.equals(anterior.operacao()) || !assinatura.equals(anterior.assinatura())) {
                throw new RegraException(HttpStatus.CONFLICT, "CHAVE_REUTILIZADA",
                        "Esta confirmação já foi usada em outra operação.");
            }
            return json.readValue(Objects.requireNonNull(anterior.resposta()), tipo);
        }
        T resultado = acao.get();
        jdbc.sql("""
                UPDATE operacao_idempotente SET resposta = :resposta
                WHERE negocio_id = :negocio AND chave = :chave
                """).param("resposta", json.writeValueAsString(resultado))
                .param("negocio", negocioId).param("chave", chave).update();
        return resultado;
    }

    private static String assinatura(String entrada) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(entrada.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException erro) {
            throw new IllegalStateException("SHA-256 indisponível.", erro);
        }
    }

    private record Registro(String operacao, String assinatura, String resposta) {
    }
}
