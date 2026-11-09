package br.com.doceup.estoque.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "insumo")
public class Insumo {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false, length = 100)
    private String nome;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Unidade unidade;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private TipoInsumo tipo;
    @Column(nullable = false, precision = 15, scale = 3)
    private BigDecimal quantidade;
    @Column(precision = 20, scale = 6)
    private BigDecimal ultimoCustoUnitario;
    @Version
    private Long versao;

    protected Insumo() {}

    public Insumo(UUID negocioId, String nome, Unidade unidade, TipoInsumo tipo) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.nome = nome;
        this.unidade = unidade;
        this.tipo = tipo;
        this.quantidade = BigDecimal.ZERO;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public Unidade getUnidade() { return unidade; }
    public TipoInsumo getTipo() { return tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public BigDecimal getUltimoCustoUnitario() { return ultimoCustoUnitario; }

    public void ajustar(BigDecimal saldo) {
        quantidade = saldo;
    }

    public void adicionar(BigDecimal quantidade, BigDecimal custoUnitario) {
        this.quantidade = this.quantidade.add(quantidade);
        if (custoUnitario != null && tipo == TipoInsumo.INGREDIENTE) {
            ultimoCustoUnitario = custoUnitario;
        }
    }

    public void consumir(BigDecimal quantidade) {
        this.quantidade = this.quantidade.subtract(quantidade);
    }
}
