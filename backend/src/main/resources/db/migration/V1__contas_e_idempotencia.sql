CREATE TABLE negocio (
    id UUID PRIMARY KEY
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY,
    emissor VARCHAR(255) NOT NULL,
    identificador VARCHAR(255) NOT NULL,
    negocio_id UUID NOT NULL UNIQUE REFERENCES negocio(id),
    UNIQUE (emissor, identificador)
);

CREATE TABLE operacao_idempotente (
    negocio_id UUID NOT NULL REFERENCES negocio(id),
    chave UUID NOT NULL,
    operacao VARCHAR(200) NOT NULL,
    assinatura CHAR(64) NOT NULL,
    resposta TEXT,
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (negocio_id, chave)
);
