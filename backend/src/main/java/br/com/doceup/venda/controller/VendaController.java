package br.com.doceup.venda.controller;

import br.com.doceup.shared.Idempotencia;
import br.com.doceup.usuario.service.ContaAtual;
import br.com.doceup.venda.dto.NovaVenda;
import br.com.doceup.venda.dto.PaginaVendas;
import br.com.doceup.venda.dto.VendaResposta;
import br.com.doceup.venda.service.VendaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/vendas")
public class VendaController {
    private final ContaAtual contaAtual;
    private final VendaService vendas;
    private final Idempotencia idempotencia;

    public VendaController(ContaAtual contaAtual, VendaService vendas, Idempotencia idempotencia) {
        this.contaAtual = contaAtual;
        this.vendas = vendas;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    public PaginaVendas listar(
            @AuthenticationPrincipal OidcUser usuario,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return vendas.listar(contaAtual.requireNegocioId(usuario), page, size);
    }

    @GetMapping("/{id}")
    public VendaResposta buscar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id) {
        return vendas.buscar(contaAtual.requireNegocioId(usuario), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VendaResposta registrar(
            @AuthenticationPrincipal OidcUser usuario,
            @RequestHeader("Idempotency-Key") UUID chave,
            @Valid @RequestBody NovaVenda nova
    ) {
        var negocioId = contaAtual.requireNegocioId(usuario);
        return idempotencia.executar(negocioId, chave, "venda:registrar", nova,
                VendaResposta.class, () -> vendas.registrar(negocioId, nova));
    }

    @PostMapping("/{id}/recebimento")
    public VendaResposta receber(
            @AuthenticationPrincipal OidcUser usuario,
            @RequestHeader("Idempotency-Key") UUID chave,
            @PathVariable UUID id
    ) {
        var negocioId = contaAtual.requireNegocioId(usuario);
        return idempotencia.executar(negocioId, chave, "venda:receber:" + id, id,
                VendaResposta.class, () -> vendas.receber(negocioId, id));
    }
}
