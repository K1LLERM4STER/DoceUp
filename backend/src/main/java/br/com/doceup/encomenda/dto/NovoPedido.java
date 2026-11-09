package br.com.doceup.encomenda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record NovoPedido(
        @NotBlank @Size(max = 100) String cliente,
        @NotNull LocalDate dataEntrega,
        @NotEmpty @Size(max = 50) List<@NotNull @Valid Item> itens,
        @Size(max = 500) String observacao
) {
    public record Item(
            @NotNull UUID produtoId,
            @NotNull @Positive @Max(1_000_000) Integer quantidade
    ) {
    }
}
