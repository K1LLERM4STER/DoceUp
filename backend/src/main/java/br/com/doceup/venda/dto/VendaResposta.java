package br.com.doceup.venda.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record VendaResposta(
        UUID id,
        LocalDate dataVenda,
        List<Item> itens,
        BigDecimal valorTotal,
        boolean pagamentoPendente,
        LocalDate dataRecebimento,
        String observacao
) {
    public record Item(UUID produtoId, String nome, int quantidade, BigDecimal precoUnitario) {
    }
}
