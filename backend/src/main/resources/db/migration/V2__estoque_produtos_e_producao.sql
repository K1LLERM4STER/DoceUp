CREATE TABLE insumo (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    nome VARCHAR(100) NOT NULL CHECK (length(trim(nome)) > 0),
    unidade VARCHAR(12) NOT NULL CHECK (unidade IN ('KG', 'L', 'UN', 'PACOTE')),
    tipo VARCHAR(12) NOT NULL CHECK (tipo IN ('INGREDIENTE', 'EMBALAGEM')),
    quantidade NUMERIC(15, 3) NOT NULL,
    ultimo_custo_unitario NUMERIC(20, 6) CHECK (ultimo_custo_unitario >= 0),
    versao BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, negocio_id),
    CHECK ((tipo = 'EMBALAGEM' AND unidade = 'UN' AND nome = 'Embalagens' AND ultimo_custo_unitario IS NULL)
        OR (tipo = 'INGREDIENTE' AND lower(nome) <> 'embalagens'))
);

CREATE UNIQUE INDEX insumo_negocio_nome_idx ON insumo (negocio_id, lower(nome));
CREATE UNIQUE INDEX insumo_embalagem_idx ON insumo (negocio_id) WHERE tipo = 'EMBALAGEM';

INSERT INTO insumo (id, negocio_id, nome, unidade, tipo, quantidade)
SELECT gen_random_uuid(), id, 'Embalagens', 'UN', 'EMBALAGEM', 0 FROM negocio;

CREATE FUNCTION criar_embalagens_do_negocio() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    INSERT INTO insumo (id, negocio_id, nome, unidade, tipo, quantidade)
    VALUES (gen_random_uuid(), NEW.id, 'Embalagens', 'UN', 'EMBALAGEM', 0);
    RETURN NEW;
END;
$$;

CREATE TRIGGER negocio_criar_embalagens AFTER INSERT ON negocio
FOR EACH ROW EXECUTE FUNCTION criar_embalagens_do_negocio();

CREATE TABLE compra_insumo (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    insumo_id UUID NOT NULL,
    quantidade NUMERIC(15, 3) NOT NULL CHECK (quantidade > 0),
    valor_total NUMERIC(14, 2) NOT NULL CHECK (valor_total >= 0),
    data TIMESTAMPTZ NOT NULL,
    ordem BIGINT GENERATED ALWAYS AS IDENTITY,
    FOREIGN KEY (insumo_id, negocio_id) REFERENCES insumo (id, negocio_id)
);

CREATE INDEX compra_insumo_negocio_data_idx ON compra_insumo (negocio_id, data);
CREATE INDEX compra_insumo_ultimo_preco_idx ON compra_insumo (negocio_id, insumo_id, data DESC, ordem DESC)
WHERE valor_total > 0;

CREATE TABLE produto (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    nome VARCHAR(100) NOT NULL CHECK (length(trim(nome)) > 0),
    preco_venda NUMERIC(14, 2) NOT NULL CHECK (preco_venda >= 0),
    quantidade_total INTEGER NOT NULL CHECK (quantidade_total >= 0),
    quantidade_reservada INTEGER NOT NULL CHECK (quantidade_reservada BETWEEN 0 AND quantidade_total),
    custo_unitario NUMERIC(35, 6) CHECK (custo_unitario >= 0),
    tem_foto BOOLEAN NOT NULL,
    ativo BOOLEAN NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    UNIQUE (id, negocio_id)
);

CREATE UNIQUE INDEX produto_negocio_nome_idx ON produto (negocio_id, lower(nome));

CREATE TABLE produto_foto (
    produto_id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL,
    formato VARCHAR(20) NOT NULL CHECK (formato IN ('image/jpeg', 'image/png')),
    conteudo BYTEA NOT NULL CHECK (octet_length(conteudo) BETWEEN 1 AND 2097152),
    FOREIGN KEY (produto_id, negocio_id) REFERENCES produto (id, negocio_id)
);

CREATE TABLE movimento_estoque (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    insumo_id UUID,
    produto_id UUID,
    tipo VARCHAR(24) NOT NULL,
    quantidade NUMERIC(17, 3) NOT NULL CHECK (quantidade <> 0),
    operacao_id UUID NOT NULL,
    motivo VARCHAR(300),
    data TIMESTAMPTZ NOT NULL,
    CHECK ((insumo_id IS NOT NULL) <> (produto_id IS NOT NULL)),
    FOREIGN KEY (insumo_id, negocio_id) REFERENCES insumo (id, negocio_id),
    FOREIGN KEY (produto_id, negocio_id) REFERENCES produto (id, negocio_id)
);

CREATE INDEX movimento_estoque_negocio_data_idx ON movimento_estoque (negocio_id, data);

CREATE TABLE producao (
    id UUID PRIMARY KEY,
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    produto_id UUID NOT NULL,
    data TIMESTAMPTZ NOT NULL,
    ordem BIGINT GENERATED ALWAYS AS IDENTITY,
    rendimento INTEGER NOT NULL CHECK (rendimento > 0),
    quantidade_embalagem NUMERIC(15, 3) NOT NULL CHECK (quantidade_embalagem >= 0),
    custo_embalagem NUMERIC(14, 2) NOT NULL CHECK (custo_embalagem >= 0),
    custo_total NUMERIC(35, 9) NOT NULL CHECK (custo_total >= custo_embalagem),
    custo_unitario NUMERIC(35, 6) NOT NULL CHECK (custo_unitario >= 0),
    UNIQUE (id, negocio_id),
    FOREIGN KEY (produto_id, negocio_id) REFERENCES produto (id, negocio_id)
);

CREATE INDEX producao_ultima_idx ON producao (negocio_id, produto_id, data DESC, ordem DESC);

CREATE TABLE producao_item (
    producao_id UUID NOT NULL,
    negocio_id UUID NOT NULL,
    insumo_id UUID NOT NULL,
    posicao INTEGER NOT NULL CHECK (posicao >= 0),
    quantidade NUMERIC(15, 3) NOT NULL CHECK (quantidade > 0),
    custo_unitario NUMERIC(20, 6) NOT NULL CHECK (custo_unitario >= 0),
    PRIMARY KEY (producao_id, posicao),
    UNIQUE (producao_id, insumo_id),
    FOREIGN KEY (producao_id, negocio_id) REFERENCES producao (id, negocio_id),
    FOREIGN KEY (insumo_id, negocio_id) REFERENCES insumo (id, negocio_id)
);
