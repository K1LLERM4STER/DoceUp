package br.com.doceup.venda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record NovaVenda(
        @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> itens,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal valorTotal,
        Boolean pagamentoPendente,
        @Size(max = 500) String observacao
) {
    public NovaVenda {
        pagamentoPendente = Boolean.TRUE.equals(pagamentoPendente);
    }

    public record Item(@NotNull UUID produtoId, @Positive @Max(1_000_000) int quantidade) {
    }
}
