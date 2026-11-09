package br.com.doceup.produto.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.UUID;

public final class ProdutoDtos {
    private ProdutoDtos() {}

    public record Formulario(@NotBlank @Size(max = 100) String nome,
                             @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal precoVenda) {}

    public record Ajuste(@NotNull @PositiveOrZero @Max(1_000_000) Integer quantidade,
                         @Size(max = 300) String motivo) {}

    public record Produto(UUID id, String nome, BigDecimal precoVenda, int quantidadeTotal,
                          int quantidadeReservada, int quantidadeDisponivel, BigDecimal custoUnitario,
                          String fotoUrl, boolean ativo) {}

    public record Imagem(String formato, byte[] conteudo) {}
}
