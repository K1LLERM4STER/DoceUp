package br.com.doceup.encomenda.controller;

import br.com.doceup.encomenda.dto.NovoPedido;
import br.com.doceup.encomenda.dto.PaginaPedidos;
import br.com.doceup.encomenda.dto.PedidoResposta;
import br.com.doceup.encomenda.entity.StatusPedido;
import br.com.doceup.encomenda.service.PedidoService;
import br.com.doceup.shared.Idempotencia;
import br.com.doceup.usuario.service.ContaAtual;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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

@RestController
@RequestMapping("/api/v1/pedidos")
public class PedidoController {
    private final ContaAtual contas;
    private final PedidoService pedidos;
    private final Idempotencia idempotencia;

    public PedidoController(ContaAtual contas, PedidoService pedidos, Idempotencia idempotencia) {
        this.contas = contas;
        this.pedidos = pedidos;
        this.idempotencia = idempotencia;
    }

    @GetMapping
    public PaginaPedidos listar(@AuthenticationPrincipal OidcUser usuario,
                                 @RequestParam(required = false) StatusPedido status,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                                 LocalDate dataEntrega,
                                 @RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
        return pedidos.listar(contas.requireNegocioId(usuario), status, dataEntrega, page, size);
    }

    @GetMapping("/{id}")
    public PedidoResposta buscar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id) {
        return pedidos.buscar(contas.requireNegocioId(usuario), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoResposta reservar(@AuthenticationPrincipal OidcUser usuario,
                                   @RequestHeader("Idempotency-Key") UUID chave,
                                   @Valid @RequestBody NovoPedido novo) {
        UUID negocio = contas.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "pedido:reservar", novo,
                PedidoResposta.class, () -> pedidos.reservar(negocio, novo));
    }

    @PostMapping("/{id}/conclusao")
    public PedidoResposta concluir(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
                                   @RequestHeader("Idempotency-Key") UUID chave) {
        UUID negocio = contas.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "pedido:concluir:" + id, id,
                PedidoResposta.class, () -> pedidos.concluir(negocio, id));
    }

    @PostMapping("/{id}/cancelamento")
    public PedidoResposta cancelar(@AuthenticationPrincipal OidcUser usuario, @PathVariable UUID id,
                                   @RequestHeader("Idempotency-Key") UUID chave) {
        UUID negocio = contas.requireNegocioId(usuario);
        return idempotencia.executar(negocio, chave, "pedido:cancelar:" + id, id,
                PedidoResposta.class, () -> pedidos.cancelar(negocio, id));
    }
}
