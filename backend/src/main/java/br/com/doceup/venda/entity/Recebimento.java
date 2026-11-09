package br.com.doceup.venda.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "recebimento")
public class Recebimento {
    @Id
    @Column(name = "venda_id")
    private UUID vendaId;

    @Column(name = "negocio_id", nullable = false, updatable = false)
    private UUID negocioId;

    @Column(name = "recebido_em", nullable = false, updatable = false)
    private Instant recebidoEm;

    @Column(nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal valor;

    protected Recebimento() {
    }

    public Recebimento(Venda venda, Instant recebidoEm) {
        this.vendaId = venda.getId();
        this.negocioId = venda.getNegocioId();
        this.recebidoEm = recebidoEm;
        this.valor = venda.getValorTotal();
    }

    public UUID getVendaId() {
        return vendaId;
    }

    public Instant getRecebidoEm() {
        return recebidoEm;
    }
}
