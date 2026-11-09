package br.com.doceup.estoque.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimento_estoque")
public class MovimentoEstoque {
    @Id
    private UUID id;
    @Column(nullable = false)
    private UUID negocioId;
    private UUID insumoId;
    private UUID produtoId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private TipoMovimento tipo;
    @Column(nullable = false, precision = 17, scale = 3)
    private BigDecimal quantidade;
    @Column(nullable = false)
    private UUID operacaoId;
    @Column(length = 300)
    private String motivo;
    @Column(nullable = false)
    private Instant data;

    protected MovimentoEstoque() {}

    public MovimentoEstoque(UUID negocioId, UUID insumoId, UUID produtoId, TipoMovimento tipo,
                            BigDecimal quantidade, UUID operacaoId, String motivo, Instant data) {
        this.id = UUID.randomUUID();
        this.negocioId = negocioId;
        this.insumoId = insumoId;
        this.produtoId = produtoId;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.operacaoId = operacaoId;
        this.motivo = motivo;
        this.data = data;
    }
}
