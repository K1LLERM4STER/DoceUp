package br.com.doceup.exception;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

@Component
public class Problemas {
    private final JsonMapper json;

    public Problemas(JsonMapper json) {
        this.json = json;
    }

    public static ProblemDetail criar(HttpStatus status, String codigo, String mensagem) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(status, mensagem);
        problema.setProperty("codigo", codigo);
        problema.setProperty("correlationId", MDC.get("correlationId"));
        return problema;
    }

    public void responder(HttpServletResponse resposta, HttpStatus status, String codigo, String mensagem)
            throws IOException {
        resposta.setStatus(status.value());
        resposta.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        json.writeValue(resposta.getOutputStream(), criar(status, codigo, mensagem));
    }
}
