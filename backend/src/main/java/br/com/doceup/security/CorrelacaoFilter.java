package br.com.doceup.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

class CorrelacaoFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest requisicao, HttpServletResponse resposta, FilterChain cadeia)
            throws ServletException, IOException {
        String id = UUID.randomUUID().toString();
        resposta.setHeader("X-Correlation-Id", id);
        try (var escopo = MDC.putCloseable("correlationId", id)) {
            cadeia.doFilter(requisicao, resposta);
        }
    }
}
