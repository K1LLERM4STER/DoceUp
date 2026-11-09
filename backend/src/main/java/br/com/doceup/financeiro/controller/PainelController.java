package br.com.doceup.financeiro.controller;

import br.com.doceup.financeiro.dto.PainelResposta;
import br.com.doceup.financeiro.service.PainelService;
import br.com.doceup.usuario.service.ContaAtual;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/painel")
public class PainelController {
    private final ContaAtual contaAtual;
    private final PainelService painel;

    public PainelController(ContaAtual contaAtual, PainelService painel) {
        this.contaAtual = contaAtual;
        this.painel = painel;
    }

    @GetMapping
    public PainelResposta consultar(
            @AuthenticationPrincipal OidcUser usuario,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String dia
    ) {
        return painel.consultar(contaAtual.requireNegocioId(usuario), mes, dia);
    }
}
