package br.com.doceup.estoque.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "compra_insumo")
public class CompraInsumo {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false)
    private UUID insumoId;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal valorTotal;
    @Column(nullable = false)
    private Instant data;
    @Column(nullable = false, insertable = false, updatable = false)
    private Long ordem;

    protected CompraInsumo() {}

    public CompraInsumo(UUID negocioId, UUID insumoId, BigDecimal quantidade, BigDecimal valorTotal,
                         Instant data) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.insumoId = insumoId;
        this.quantidade = quantidade;
        this.valorTotal = valorTotal;
        this.data = data;
    }

    public UUID getId() { return id; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getValorTotal() { return valorTotal; }
}
