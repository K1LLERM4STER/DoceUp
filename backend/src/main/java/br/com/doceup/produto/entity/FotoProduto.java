package br.com.doceup.produto.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "produto_foto")
public class FotoProduto {
    @Id
    private UUID produtoId;
    @Column(nullable = false)
    private UUID negocioId;
    @Column(nullable = false, length = 20)
    private String formato;
    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] conteudo;

    protected FotoProduto() {}

    public FotoProduto(UUID produtoId, UUID negocioId, String formato, byte[] conteudo) {
        this.produtoId = produtoId;
        this.negocioId = negocioId;
        substituir(formato, conteudo);
    }

    public String getFormato() { return formato; }
    public byte[] getConteudo() { return conteudo; }

    public void substituir(String formato, byte[] conteudo) {
        this.formato = formato;
        this.conteudo = conteudo;
    }
}
