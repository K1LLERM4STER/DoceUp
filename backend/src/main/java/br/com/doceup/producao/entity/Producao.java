package br.com.doceup.producao.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "producao")
public class Producao {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false)
    private UUID produtoId;
    @Column(nullable = false)
    private Instant data;
    @Column(nullable = false, insertable = false, updatable = false)
    private Long ordem;
    @Column(nullable = false)
    private int rendimento;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidadeEmbalagem;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal custoEmbalagem;
    @Column(nullable = false, precision = 35, scale = 9)
    private BigDecimal custoTotal;
    @Column(nullable = false, precision = 35, scale = 6)
    private BigDecimal custoUnitario;
    @ElementCollection
    @CollectionTable(name = "producao_item", joinColumns = @JoinColumn(name = "producao_id"))
    @OrderColumn(name = "posicao")
    private List<ItemProducao> ingredientes;

    protected Producao() {}

    public Producao(UUID negocioId, UUID produtoId, Instant data, int rendimento, BigDecimal quantidadeEmbalagem,
                    BigDecimal custoEmbalagem, BigDecimal custoTotal, BigDecimal custoUnitario,
                    List<ItemProducao> ingredientes) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.produtoId = produtoId;
        this.data = data;
        this.rendimento = rendimento;
        this.quantidadeEmbalagem = quantidadeEmbalagem;
        this.custoEmbalagem = custoEmbalagem;
        this.custoTotal = custoTotal;
        this.custoUnitario = custoUnitario;
        this.ingredientes = new ArrayList<>(ingredientes);
    }

    public UUID getId() { return id; }
    public int getRendimento() { return rendimento; }
    public BigDecimal getQuantidadeEmbalagem() { return quantidadeEmbalagem; }
    public BigDecimal getCustoEmbalagem() { return custoEmbalagem; }
    public List<ItemProducao> getIngredientes() { return ingredientes; }
}
