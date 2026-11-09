package br.com.doceup.producao.controller;

import br.com.doceup.producao.dto.ProducaoDtos;
import br.com.doceup.producao.service.ProducaoService;
import br.com.doceup.shared.Idempotencia;
import br.com.doceup.usuario.service.ContaAtual;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/producao/{produtoId}")
public class ProducaoController {
    private final ProducaoService producoes;
    private final ContaAtual conta;
    private final Idempotencia idempotencia;

    public ProducaoController(ProducaoService producoes, ContaAtual conta, Idempotencia idempotencia) {
        this.producoes = producoes;
        this.conta = conta;
        this.idempotencia = idempotencia;
    }

    @GetMapping("/ultima")
    public ResponseEntity<ProducaoDtos.Base> ultima(@AuthenticationPrincipal OidcUser usuario,
            @PathVariable UUID produtoId) {
        return producoes.ultima(conta.requireNegocioId(usuario), produtoId)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/previa")
    public ProducaoDtos.Resumo previa(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID produtoId,
            @Valid @RequestBody ProducaoDtos.Entrada entrada) {
        return producoes.previa(conta.requireNegocioId(usuario), produtoId, entrada);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProducaoDtos.Resumo confirmar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID produtoId,
            @RequestHeader("Idempotency-Key") UUID chave, @Valid @RequestBody ProducaoDtos.Entrada entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "producao.confirmar:" + produtoId, entrada,
                ProducaoDtos.Resumo.class, () -> producoes.confirmar(negocio, produtoId, entrada));
    }
}
