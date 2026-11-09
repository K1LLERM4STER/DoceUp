package br.com.doceup.usuario.controller;

import br.com.doceup.usuario.dto.CsrfDto;
import br.com.doceup.usuario.dto.SessaoDto;
import br.com.doceup.usuario.service.ContaAtual;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
class AuthController {
    private final ContaAtual conta;

    AuthController(ContaAtual conta) {
        this.conta = conta;
    }

    @GetMapping("/me")
    ResponseEntity<SessaoDto> sessao(@AuthenticationPrincipal OidcUser usuario) {
        conta.requireNegocioId(usuario);
        String nome = usuario.getGivenName();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new SessaoDto(nome == null || nome.isBlank() ? "Meu negócio" : nome));
    }

    @GetMapping("/csrf")
    ResponseEntity<CsrfDto> csrf(CsrfToken csrf) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(new CsrfDto(csrf.getToken(), csrf.getHeaderName()));
    }
}
