CREATE TABLE venda (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    vendida_em TIMESTAMPTZ NOT NULL,
    valor_total NUMERIC(19, 2) NOT NULL CHECK (valor_total >= 0),
    observacao VARCHAR(500),
    UNIQUE (id, negocio_id)
);

CREATE INDEX venda_negocio_data_idx ON venda (negocio_id, vendida_em DESC, id DESC);

CREATE TABLE venda_item (
    venda_id UUID NOT NULL REFERENCES venda(id),
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    produto_id UUID NOT NULL REFERENCES produto(id),
    nome VARCHAR(120) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    preco_unitario NUMERIC(19, 2) NOT NULL CHECK (preco_unitario >= 0),
    PRIMARY KEY (venda_id, posicao),
    UNIQUE (venda_id, produto_id)
);

CREATE TABLE recebimento (
    venda_id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL,
    recebido_em TIMESTAMPTZ NOT NULL,
    valor NUMERIC(19, 2) NOT NULL CHECK (valor >= 0),
    FOREIGN KEY (venda_id, negocio_id) REFERENCES venda(id, negocio_id)
);

CREATE INDEX recebimento_negocio_data_idx ON recebimento (negocio_id, recebido_em);
