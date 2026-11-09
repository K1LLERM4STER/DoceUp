package br.com.doceup.produto.entity;

import br.com.doceup.exception.RegraException;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;

@Entity
@Table(name = "produto")
public class Produto {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false, length = 100)
    private String nome;
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precoVenda;
    @Column(nullable = false)
    private int quantidadeTotal;
    @Column(nullable = false)
    private int quantidadeReservada;
    @Column(precision = 35, scale = 6)
    private BigDecimal custoUnitario;
    @Column(nullable = false)
    private boolean temFoto;
    @Column(nullable = false)
    private boolean ativo;
    @Version
    private Long versao;

    protected Produto() {}

    public Produto(UUID negocioId, String nome, BigDecimal precoVenda) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.nome = nome;
        this.precoVenda = precoVenda;
        this.ativo = true;
    }

    public UUID getId() { return id; }
    public String getNome() { return nome; }
    public BigDecimal getPrecoVenda() { return precoVenda; }
    public int getQuantidadeTotal() { return quantidadeTotal; }
    public int getQuantidadeReservada() { return quantidadeReservada; }
    public int getQuantidadeDisponivel() { return quantidadeTotal - quantidadeReservada; }
    public BigDecimal getCustoUnitario() { return custoUnitario; }
    public boolean isTemFoto() { return temFoto; }
    public boolean isAtivo() { return ativo; }

    public void editar(String nome, BigDecimal precoVenda) {
        this.nome = nome;
        this.precoVenda = precoVenda;
    }

    public void registrarFoto() {
        temFoto = true;
    }

    public void ajustar(int quantidade) {
        if (quantidade < quantidadeReservada) {
            throw new RegraException(HttpStatus.CONFLICT, "ESTOQUE_RESERVADO",
                    "O estoque total não pode ficar abaixo da quantidade separada.");
        }
        quantidadeTotal = quantidade;
    }

    public void produzir(int quantidade, BigDecimal custoUnitario) {
        exigirAtivo();
        if (quantidade > Integer.MAX_VALUE - quantidadeTotal) {
            throw new RegraException(HttpStatus.CONFLICT, "LIMITE_ESTOQUE",
                    "O estoque ultrapassaria a quantidade máxima.");
        }
        quantidadeTotal += quantidade;
        this.custoUnitario = custoUnitario;
    }

    public void vender(int quantidade) {
        exigirDisponivel(quantidade);
        quantidadeTotal -= quantidade;
    }

    public void reservar(int quantidade) {
        exigirDisponivel(quantidade);
        quantidadeReservada += quantidade;
    }

    public void liberar(int quantidade) {
        if (quantidade <= 0 || quantidade > quantidadeReservada) {
            throw new RegraException(HttpStatus.CONFLICT, "RESERVA_INSUFICIENTE",
                    "A quantidade separada não é suficiente.");
        }
        quantidadeReservada -= quantidade;
    }

    public void entregarReserva(int quantidade) {
        liberar(quantidade);
        quantidadeTotal -= quantidade;
    }

    public void desativar() {
        if (quantidadeReservada > 0) {
            throw new RegraException(HttpStatus.CONFLICT, "ESTOQUE_RESERVADO",
                    "Conclua ou cancele as encomendas desse produto antes de desativá-lo.");
        }
        ativo = false;
    }

    public void exigirAtivo() {
        if (!ativo) {
            throw new RegraException(HttpStatus.CONFLICT, "PRODUTO_INATIVO", "Esse produto está desativado.");
        }
    }

    private void exigirDisponivel(int quantidade) {
        exigirAtivo();
        if (quantidade <= 0 || quantidade > getQuantidadeDisponivel()) {
            throw new RegraException(HttpStatus.CONFLICT, "ESTOQUE_INSUFICIENTE",
                    "Não há unidades disponíveis suficientes de " + nome + ".");
        }
    }
}
