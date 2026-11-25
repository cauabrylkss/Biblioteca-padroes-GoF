-- Schema do perfil "producao" (MySQL) — R8, D-06.
-- Executado pela própria aplicação na primeira conexão (UnidadeDeTrabalhoJdbc). Só cria o que falta:
-- rodar de novo não apaga nem altera dados.
-- O banco e o usuário são criados antes, uma única vez, por docs/mysql/criar-banco.sql.
--
-- Chaves estrangeiras só entre papel e pessoa (excluir pessoa com papel já é recusado pelo Relacionamento).
-- Transações e multas guardam ids sem chave estrangeira: são histórico e continuam valendo depois que um livro
-- ou um estudante é excluído.

CREATE TABLE IF NOT EXISTS livro (
    id        BIGINT       NOT NULL PRIMARY KEY,   -- informado no cadastro (B-22)
    titulo    VARCHAR(200) NOT NULL,
    situacao  VARCHAR(20)  NOT NULL                -- nome do estado: LIVRE, RESERVADO, ... (D-11)
);

CREATE TABLE IF NOT EXISTS pessoa (
    id            BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    nome          VARCHAR(100) NOT NULL,
    sobrenome     VARCHAR(100) NOT NULL,
    cpf           VARCHAR(11)  NOT NULL UNIQUE,     -- só dígitos (B-22, B-23)
    data_criacao  DATETIME(6)  NOT NULL
);

CREATE TABLE IF NOT EXISTS estudante (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_pessoa  BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_estudante_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id)
);

CREATE TABLE IF NOT EXISTS atendente (
    id         BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_pessoa  BIGINT NOT NULL UNIQUE,
    CONSTRAINT fk_atendente_pessoa FOREIGN KEY (id_pessoa) REFERENCES pessoa (id)
);

-- Uma tabela para as três transações de circulação (Reserva, Emprestimo, Devolucao), separadas pela coluna tipo.
-- Colunas que não se aplicam a um tipo ficam nulas (ex.: reserva não tem atendente nem vencimento).
CREATE TABLE IF NOT EXISTS transacao (
    id              BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tipo            VARCHAR(12) NOT NULL,           -- RESERVA | EMPRESTIMO | DEVOLUCAO
    id_livro        BIGINT      NOT NULL,
    id_estudante    BIGINT      NOT NULL,
    data_transacao  DATE        NOT NULL,
    id_atendente    BIGINT      NULL,
    vencimento      DATE        NULL,
    num_renovacoes  INT         NOT NULL DEFAULT 0,
    id_emprestimo   BIGINT      NULL,               -- só na devolução: qual empréstimo ela encerra
    INDEX ix_transacao_livro (id_livro, tipo),
    INDEX ix_transacao_emprestimo (id_emprestimo)
);

-- Multa com o boleto (1:1). Valor e política gravados na emissão e nunca alterados (R2).
CREATE TABLE IF NOT EXISTS multa (
    id                    BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    id_emprestimo         BIGINT        NOT NULL,
    id_estudante          BIGINT        NOT NULL,
    dias_de_atraso        BIGINT        NOT NULL,
    valor                 DECIMAL(10,2) NOT NULL,   -- BigDecimal, nunca double
    codigo_politica       VARCHAR(30)   NOT NULL,
    data_emissao          DATE          NOT NULL,
    estado                VARCHAR(10)   NOT NULL,   -- EMITIDA | QUITADA
    boleto_banco          VARCHAR(10)   NULL,
    boleto_identificador  VARCHAR(40)   NULL,
    boleto_valor          DECIMAL(10,2) NULL,
    boleto_situacao       VARCHAR(10)   NULL,       -- EMITIDO | PAGO
    INDEX ix_multa_emprestimo (id_emprestimo),
    INDEX ix_multa_estudante (id_estudante),
    INDEX ix_multa_boleto (boleto_banco, boleto_identificador)
);

-- Notificações de pagamento já processadas: a chave primária impede processar a mesma duas vezes (R3).
CREATE TABLE IF NOT EXISTS notificacao_processada (
    banco           VARCHAR(10) NOT NULL,
    id_notificacao  VARCHAR(60) NOT NULL,
    PRIMARY KEY (banco, id_notificacao)
);
