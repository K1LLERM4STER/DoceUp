package br.com.doceup.produto.controller;

import br.com.doceup.estoque.dto.PaginaEstoque;
import br.com.doceup.produto.dto.ProdutoDtos;
import br.com.doceup.produto.service.ProdutoService;
import br.com.doceup.shared.Idempotencia;
import br.com.doceup.usuario.service.ContaAtual;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/produtos")
public class ProdutoController {
    private final ProdutoService produtos;
    private final ContaAtual conta;
    private final Idempotencia idempotencia;

    public ProdutoController(ProdutoService produtos, ContaAtual conta, Idempotencia idempotencia) {
        this.produtos = produtos;
        this.conta = conta;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    public PaginaEstoque<ProdutoDtos.Produto> listar(@AuthenticationPrincipal OidcUser usuario,
            @RequestParam(defaultValue = "") @Size(max = 100) String busca, Pageable pagina) {
        return produtos.listar(conta.requireNegocioId(usuario), busca, pagina);
    }

    @GetMapping("/{id}")
    public ProdutoDtos.Produto obter(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id) {
        return produtos.obter(conta.requireNegocioId(usuario), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProdutoDtos.Produto criar(@AuthenticationPrincipal OidcUser usuario,
            @RequestHeader("Idempotency-Key") UUID chave, @Valid @RequestBody ProdutoDtos.Formulario entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "produtos.criar", entrada, ProdutoDtos.Produto.class,
                () -> produtos.criar(negocio, entrada));
    }

    @PutMapping("/{id}")
    public ProdutoDtos.Produto editar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID chave, @Valid @RequestBody ProdutoDtos.Formulario entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "produtos.editar:" + id, entrada, ProdutoDtos.Produto.class,
                () -> produtos.editar(negocio, id, entrada));
    }

    @PostMapping("/{id}/ajustes")
    public ProdutoDtos.Produto ajustar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID chave, @Valid @RequestBody ProdutoDtos.Ajuste entrada) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "produtos.ajustar:" + id, entrada, ProdutoDtos.Produto.class,
                () -> produtos.ajustar(negocio, id, entrada));
    }

    @PostMapping("/{id}/desativar")
    public ProdutoDtos.Produto desativar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") UUID chave) {
        UUID negocio = conta.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "produtos.desativar:" + id, id, ProdutoDtos.Produto.class,
                () -> produtos.desativar(negocio, id));
    }

    @PutMapping(value = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProdutoDtos.Produto salvarFoto(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
            @RequestParam("file") MultipartFile arquivo) {
        return produtos.salvarFoto(conta.requireNegocioId(usuario), id, arquivo);
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> obterFoto(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id) {
        ProdutoDtos.Imagem foto = produtos.obterFoto(conta.requireNegocioId(usuario), id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.formato())).cacheControl(CacheControl.noStore())
                .body(foto.conteudo());
    }
}
