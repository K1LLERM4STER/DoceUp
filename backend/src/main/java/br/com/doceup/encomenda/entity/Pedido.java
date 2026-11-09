package br.com.doceup.encomenda.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pedido")
public class Pedido {
    @Id
    private UUID id;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(nullable = false, length = 100, updatable = false)
    private String cliente;

    @Column(name = "data_entrega", nullable = false, updatable = false)
    private LocalDate dataEntrega;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(length = 500, updatable = false)
    private String observacao;

    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal valorTotal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private StatusPedido status;

    @Column(name = "venda_id")
    private UUID vendaId;

    @ElementCollection
    @CollectionTable(name = "pedido_item", joinColumns = @JoinColumn(name = "pedido_id"))
    @OrderColumn(name = "posicao", nullable = false)
    private List<ItemPedido> itens = new ArrayList<>();

    protected Pedido() {
    }

    public Pedido(UUID negocioId, String cliente, LocalDate dataEntrega, Instant criadoEm,
                  String observacao, BigDecimal valorTotal, List<ItemPedido> itens) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.cliente = cliente;
        this.dataEntrega = dataEntrega;
        this.criadoEm = criadoEm;
        this.observacao = observacao;
        this.valorTotal = valorTotal;
        this.status = StatusPedido.ABERTA;
        this.itens.addAll(itens);
    }

    public UUID getId() { return id; }

    public String getCliente() { return cliente; }

    public LocalDate getDataEntrega() { return dataEntrega; }

    public String getObservacao() { return observacao; }

    public BigDecimal getValorTotal() { return valorTotal; }

    public StatusPedido getStatus() { return status; }

    public UUID getVendaId() { return vendaId; }

    public List<ItemPedido> getItens() { return List.copyOf(itens); }

    public void concluir(UUID vendaId) {
        this.vendaId = vendaId;
        this.status = StatusPedido.CONCLUIDA;
    }

    public void cancelar() {
        this.status = StatusPedido.CANCELADA;
    }
}
