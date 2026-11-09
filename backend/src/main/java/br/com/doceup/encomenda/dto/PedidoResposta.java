package br.com.doceup.encomenda.dto;

import br.com.doceup.encomenda.entity.StatusPedido;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PedidoResposta(
        UUID id,
        String cliente,
        LocalDate dataEntrega,
        List<Item> itens,
        BigDecimal valorTotal,
        StatusPedido status,
        String observacao,
        UUID vendaId
) {
    public record Item(UUID produtoId, String nome, int quantidade, BigDecimal precoUnitario) {
    }
}
