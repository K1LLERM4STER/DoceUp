CREATE TABLE pedido (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    cliente VARCHAR(100) NOT NULL CHECK (length(trim(cliente)) > 0),
    data_entrega DATE NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL,
    observacao VARCHAR(500),
    valor_total NUMERIC(19, 2) NOT NULL CHECK (valor_total >= 0),
    status VARCHAR(12) NOT NULL CHECK (status IN ('ABERTA', 'CONCLUIDA', 'CANCELADA')),
    venda_id UUID UNIQUE,
    FOREIGN KEY (venda_id, negocio_id) REFERENCES venda (id, negocio_id),
    CHECK ((status = 'CONCLUIDA') = (venda_id IS NOT NULL)),
    UNIQUE (id, negocio_id)
);

CREATE INDEX pedido_negocio_data_idx ON pedido (negocio_id, data_entrega, criado_em DESC, id);
CREATE INDEX pedido_negocio_status_data_idx ON pedido (negocio_id, status, data_entrega);

CREATE TABLE pedido_item (
    pedido_id UUID NOT NULL,
    negocio_id UUID NOT NULL,
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    produto_id UUID NOT NULL,
    nome VARCHAR(100) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(19, 2) NOT NULL CHECK (preco_unitario >= 0),
    PRIMARY KEY (pedido_id, posicao),
    UNIQUE (pedido_id, produto_id),
    FOREIGN KEY (pedido_id, negocio_id) REFERENCES pedido (id, negocio_id),
    FOREIGN KEY (produto_id, negocio_id) REFERENCES produto (id, negocio_id)
);
