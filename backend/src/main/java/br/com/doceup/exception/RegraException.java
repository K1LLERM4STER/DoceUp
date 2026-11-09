package br.com.doceup.exception;

import java.util.Map;
import org.springframework.http.HttpStatus;

public class RegraException extends RuntimeException {
    private final HttpStatus status;
    private final String codigo;
    private final Map<String, Object> detalhes;

    public RegraException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, Map.of());
    }

    public RegraException(HttpStatus status, String codigo, String mensagem, Map<String, Object> detalhes) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.detalhes = Map.copyOf(detalhes);
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCodigo() {
        return codigo;
    }

    public Map<String, Object> getDetalhes() {
        return detalhes;
    }
}
