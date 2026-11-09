package br.com.doceup.producao.dto;

import br.com.doceup.estoque.entity.Unidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class ProducaoDtos {
    private ProducaoDtos() {}

    public record Ingrediente(@NotNull UUID insumoId,
                              @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal quantidade) {}

    public record Entrada(@NotNull @Positive @Max(1_000_000) Integer rendimento,
                          @NotEmpty @Size(max = 100) List<@NotNull @Valid Ingrediente> ingredientes,
                          @PositiveOrZero @Digits(integer = 9, fraction = 3) BigDecimal quantidadeEmbalagem,
                          @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal custoEmbalagem,
                          Boolean aceitarEstoqueNegativo) {
        public Entrada {
            aceitarEstoqueNegativo = Boolean.TRUE.equals(aceitarEstoqueNegativo);
            if (quantidadeEmbalagem == null) {
                quantidadeEmbalagem = BigDecimal.ZERO;
            }
            if (custoEmbalagem == null) {
                custoEmbalagem = BigDecimal.ZERO;
            }
        }
    }

    public record Base(int rendimento, List<Ingrediente> ingredientes,
                       BigDecimal quantidadeEmbalagem, BigDecimal custoEmbalagem) {}

    public record Falta(UUID insumoId, String nome, Unidade unidade, BigDecimal disponivel,
                        BigDecimal necessario, BigDecimal falta) {}

    public record Resumo(UUID id, int rendimento, BigDecimal custoIngredientes, BigDecimal custoEmbalagem,
                         BigDecimal custoTotal, BigDecimal custoUnitario, List<Falta> faltas) {}
}
