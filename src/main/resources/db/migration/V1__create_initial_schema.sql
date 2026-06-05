CREATE TABLE clientes (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    telefone VARCHAR(30) NOT NULL,
    email VARCHAR(120) NOT NULL,
    cpf_cnpj VARCHAR(20) NOT NULL,
    tipo_pessoa VARCHAR(20) NOT NULL,
    nome_empresa VARCHAR(120),
    regime_tributario VARCHAR(80),
    segmento VARCHAR(100),
    status VARCHAR(40) NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE TABLE atendimentos (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT REFERENCES clientes(id),
    tipo_atendimento VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    etapa_atual INTEGER NOT NULL,
    dados_coletados TEXT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL
);

CREATE TABLE mensagens_chat (
    id BIGSERIAL PRIMARY KEY,
    cliente_id BIGINT REFERENCES clientes(id),
    atendimento_id BIGINT REFERENCES atendimentos(id),
    direcao VARCHAR(20) NOT NULL,
    conteudo TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_atendimentos_cliente_status ON atendimentos(cliente_id, status);
CREATE INDEX idx_mensagens_cliente_criado ON mensagens_chat(cliente_id, criado_em);
