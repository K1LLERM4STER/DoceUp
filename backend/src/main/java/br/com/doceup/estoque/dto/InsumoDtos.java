package br.com.doceup.estoque.dto;

import br.com.doceup.estoque.entity.TipoInsumo;
import br.com.doceup.estoque.entity.Unidade;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public final class InsumoDtos {
    private InsumoDtos() {}

    public record Criar(@NotBlank @Size(max = 100) String nome, @NotNull Unidade unidade, TipoInsumo tipo) {}

    public record Compra(@NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
                         @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal valorTotal,
                         LocalDate data) {}

    public record Ajuste(@NotNull @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal quantidade,
                         @Size(max = 300) String motivo) {}

    public record Insumo(UUID id, String nome, Unidade unidade, TipoInsumo tipo,
                         BigDecimal quantidade, BigDecimal ultimoCustoUnitario) {}
}
