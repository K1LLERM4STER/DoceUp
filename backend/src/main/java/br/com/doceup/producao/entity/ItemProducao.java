package br.com.doceup.producao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class ItemProducao {
    @Column(nullable = false)
    private UUID insumoId;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;
    @Column(nullable = false, precision = 20, scale = 6)
    private BigDecimal custoUnitario;

    protected ItemProducao() {}

    public ItemProducao(UUID insumoId, UUID negocioId, BigDecimal quantidade, BigDecimal custoUnitario) {
        this.insumoId = insumoId;
        this.negocioId = negocioId;
        this.quantidade = quantidade;
        this.custoUnitario = custoUnitario;
    }

    public UUID getInsumoId() { return insumoId; }
    public BigDecimal getQuantidade() { return quantidade; }

    public BigDecimal calcularCusto() {
        return quantidade.multiply(custoUnitario);
    }
}
