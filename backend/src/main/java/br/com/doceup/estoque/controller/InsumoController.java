package br.com.doceup.estoque.controller;

import br.com.doceup.estoque.dto.InsumoDtos;
import br.com.doceup.estoque.dto.PaginaEstoque;
import br.com.doceup.estoque.service.InsumoService;
import br.com.doceup.shared.Idempotencia;
import br.com.doceup.usuario.service.ContaAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/ingredientes")
public class InsumoController {
    private final InsumoService insumos;
    private final ContaAtual conta;
    private final Idempotencia idempotencia;

    public InsumoController(InsumoService insumos, ContaAtual conta, Idempotencia idempotencia) {
        this.insumos = insumos;
        this.conta = conta;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    public PaginaEstoque<InsumoDtos.Insumo> listar(@AuthenticationPrincipal OidcUser usuario,
            @RequestParam(defaultValue = "") @Size(max = 100) String busca, Pageable pagina) {
        return insumos.listar(conta.requireNegocioId(usuario), busca, pagina);
    }

    @GetMapping("/{id}")
    public InsumoDtos.Insumo obter(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id) {
        return insumos.obter(conta.requireNegocioId(usuario), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDtos.Insumo criar(@AuthenticationPrincipal OidcUser usuario,
            @RequestHeader("Idempotency-Key") UUID chave, @RequestBody @Valid InsumoDtos.Criar entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "insumos.criar", entrada, InsumoDtos.Insumo.class,
                () -> insumos.criar(negocio, entrada));
    }

    @PostMapping("/{id}/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public InsumoDtos.Insumo comprar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID chave, @RequestBody @Valid InsumoDtos.Compra entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "insumos.comprar:" + id, entrada, InsumoDtos.Insumo.class,
                () -> insumos.comprar(negocio, id, entrada));
    }

    @PostMapping("/{id}/ajustes")
    public InsumoDtos.Insumo ajustar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID chave, @RequestBody @Valid InsumoDtos.Ajuste entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "insumos.ajustar:" + id, entrada, InsumoDtos.Insumo.class,
                () -> insumos.ajustar(negocio, id, entrada));
    }
}
