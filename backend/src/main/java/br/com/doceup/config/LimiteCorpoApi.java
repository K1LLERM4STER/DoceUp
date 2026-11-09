package br.com.doceup.config;

import br.com.doceup.exception.RegraException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.RequestBodyAdviceAdapter;

@ControllerAdvice(basePackages = "br.com.doceup")
class LimiteCorpoApi extends RequestBodyAdviceAdapter {
    private static final int LIMITE_BYTES = 64 * 1024;

    @Override
    public boolean supports(MethodParameter parametro, Type tipo,
                            Class<? extends HttpMessageConverter<?>> conversor) {
        return true;
    }

    @Override
    public HttpInputMessage beforeBodyRead(HttpInputMessage entrada, MethodParameter parametro, Type tipo,
                                           Class<? extends HttpMessageConverter<?>> conversor) throws IOException {
        if (entrada.getHeaders().getContentLength() > LIMITE_BYTES) {
            throw corpoGrande();
        }
        byte[] dados = entrada.getBody().readNBytes(LIMITE_BYTES + 1);
        if (dados.length > LIMITE_BYTES) {
            throw corpoGrande();
        }
        return new HttpInputMessage() {
            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(dados);
            }

            @Override
            public HttpHeaders getHeaders() {
                return entrada.getHeaders();
            }
        };
    }

    private static RegraException corpoGrande() {
        return new RegraException(HttpStatus.PAYLOAD_TOO_LARGE, "CORPO_GRANDE", "Os dados enviados são grandes demais.");
    }
}
