package br.com.doceup.venda.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "venda")
public class Venda {
    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "vendida_em", nullable = false, updatable = false)
    private Instant vendidaEm;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorTotal;

    @Column(length = 500, updatable = false)
    private String observacao;

    @ElementCollection
    @CollectionTable(name = "venda_item", joinColumns = @JoinColumn(name = "venda_id"))
    @OrderColumn(name = "posicao", nullable = false)
    private List<ItemVenda> itens = new ArrayList<>();

    protected Venda() {
    }

    public Venda(UUID negocioId, Instant vendidaEm, BigDecimal valorTotal, String observacao, List<ItemVenda> itens) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.vendidaEm = vendidaEm;
        this.valorTotal = valorTotal;
        this.observacao = observacao;
        this.itens.addAll(itens);
    }

    public UUID getId() {
        return id;
    }

    public UUID getNegocioId() {
        return negocioId;
    }

    public Instant getVendidaEm() {
        return vendidaEm;
    }

    public BigDecimal getValorTotal() {
        return valorTotal;
    }

    public String getObservacao() {
        return observacao;
    }

    public List<ItemVenda> getItens() {
        return List.copyOf(itens);
    }
}
