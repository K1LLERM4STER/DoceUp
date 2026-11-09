package br.com.doceup.exception;

import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.ErrorResponse;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@RestControllerAdvice
class TratadorApi {
    private static final Logger LOG = LoggerFactory.getLogger(TratadorApi.class);

    @ExceptionHandler(RegraException.class)
    ProblemDetail regra(RegraException erro) {
        ProblemDetail problema = Problemas.criar(erro.getStatus(), erro.getCodigo(), erro.getMessage());
        erro.getDetalhes().forEach(problema::setProperty);
        return problema;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validacao(MethodArgumentNotValidException erro) {
        Map<String, String> campos = new LinkedHashMap<>();
        erro.getBindingResult().getFieldErrors().forEach(campo ->
                campos.putIfAbsent(campo.getField(), "Verifique este campo."));
        ProblemDetail problema = Problemas.criar(HttpStatus.BAD_REQUEST, "CAMPOS_INVALIDOS", "Revise os campos.");
        problema.setProperty("campos", campos);
        return problema;
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class, TypeMismatchException.class})
    ProblemDetail entradaInvalida() {
        return Problemas.criar(HttpStatus.BAD_REQUEST, "ENTRADA_INVALIDA", "Revise os dados enviados.");
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    ProblemDetail concorrencia() {
        return Problemas.criar(HttpStatus.CONFLICT, "DADOS_ALTERADOS",
                "Os dados mudaram. Atualize a tela e tente novamente.");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail integridade(DataIntegrityViolationException erro) {
        if (erro.getMostSpecificCause() instanceof SQLException sql && "23505".equals(sql.getSQLState())) {
            return Problemas.criar(HttpStatus.CONFLICT, "REGISTRO_DUPLICADO",
                    "Já existe um registro com esses dados.");
        }
        return inesperado(erro);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    ProblemDetail arquivoGrande() {
        return Problemas.criar(HttpStatus.PAYLOAD_TOO_LARGE, "ARQUIVO_GRANDE", "O arquivo é grande demais.");
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail acessoNegado() {
        return Problemas.criar(HttpStatus.FORBIDDEN, "ACESSO_NEGADO", "Acesso não permitido.");
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail inesperado(Exception erro) {
        if (erro instanceof ErrorResponse resposta) {
            HttpStatus status = HttpStatus.resolve(resposta.getStatusCode().value());
            if (status != null && status.is4xxClientError()) {
                return Problemas.criar(status, "REQUISICAO_INVALIDA", "Verifique a operação solicitada.");
            }
        }
        LOG.error("Falha da API do tipo {}", erro.getClass().getSimpleName());
        return Problemas.criar(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO",
                "Não deu para concluir. Tente novamente.");
    }
}
