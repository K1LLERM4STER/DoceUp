package br.com.doceup.encomenda.entity;

import br.com.doceup.produto.entity.Produto;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;

@Embeddable
public class ItemPedido {
    @Column(name = "negocio_id", nullable = false)
    private UUID negocioId;

    @Column(name = "produto_id", nullable = false)
    private UUID produtoId;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false)
    private int quantidade;

    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoUnitario;

    protected ItemPedido() {
    }

    public ItemPedido(UUID negocioId, Produto produto, int quantidade) {
        this.negocioId = negocioId;
        this.produtoId = produto.getId();
        this.nome = produto.getNome();
        this.quantidade = quantidade;
        this.precoUnitario = produto.getPrecoVenda();
    }

    public UUID getProdutoId() { return produtoId; }

    public String getNome() { return nome; }

    public int getQuantidade() { return quantidade; }

    public BigDecimal getPrecoUnitario() { return precoUnitario; }

    public BigDecimal subtotal() {
        return precoUnitario.multiply(BigDecimal.valueOf(quantidade));
    }
}
