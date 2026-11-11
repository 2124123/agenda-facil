-- Agenda Fácil - modelo físico (MySQL 8)
-- OPCIONAL: a API cria estas tabelas sozinha (spring.jpa.hibernate.ddl-auto=update).
-- Use este script para documentar/revisar o modelo ou para criar o banco manualmente
-- (nesse caso defina spring.jpa.hibernate.ddl-auto=none).

CREATE DATABASE IF NOT EXISTS agenda_facil CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE agenda_facil;

CREATE TABLE estabelecimento (
  id        BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome      VARCHAR(120) NOT NULL,
  endereco  VARCHAR(200),
  telefone  VARCHAR(20)
);

CREATE TABLE usuario (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  nome           VARCHAR(120) NOT NULL,
  email          VARCHAR(160) NOT NULL UNIQUE,
  senha          VARCHAR(255) NOT NULL,              -- hash BCrypt
  telefone       VARCHAR(20),
  perfil         VARCHAR(20)  NOT NULL,              -- CLIENTE | PROFISSIONAL | ADMINISTRADOR
  status         VARCHAR(10)  NOT NULL DEFAULT 'ATIVO',
  data_cadastro  DATETIME(6)  NOT NULL
);

CREATE TABLE cliente (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id     BIGINT NOT NULL UNIQUE,
  data_cadastro  DATETIME(6) NOT NULL,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE profissional (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  estabelecimento_id  BIGINT NOT NULL,
  usuario_id          BIGINT NOT NULL UNIQUE,
  especialidade       VARCHAR(60) NOT NULL,          -- = categoria dos serviços que atende
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id),
  FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE servico (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  estabelecimento_id  BIGINT NOT NULL,
  nome                VARCHAR(120) NOT NULL,
  descricao           VARCHAR(400),
  categoria           VARCHAR(60)  NOT NULL,
  duracao_minutos     INT          NOT NULL,
  preco               DECIMAL(10,2) NOT NULL,
  ativo               BIT NOT NULL DEFAULT 1,
  FOREIGN KEY (estabelecimento_id) REFERENCES estabelecimento(id)
);

CREATE TABLE disponibilidade (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  profissional_id  BIGINT NOT NULL,
  dia_semana       VARCHAR(12) NOT NULL,             -- MONDAY ... SUNDAY
  hora_inicio      TIME(6) NOT NULL,
  hora_fim         TIME(6) NOT NULL,
  FOREIGN KEY (profissional_id) REFERENCES profissional(id)
);

CREATE TABLE agendamento (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  cliente_id       BIGINT NOT NULL,
  profissional_id  BIGINT NOT NULL,
  servico_id       BIGINT NOT NULL,
  data             DATE    NOT NULL,
  hora_inicio      TIME(6) NOT NULL,
  hora_fim         TIME(6) NOT NULL,
  status           VARCHAR(12) NOT NULL,             -- PENDENTE | CONFIRMADO | CANCELADO | REALIZADO
  valor            DECIMAL(10,2) NOT NULL,           -- preço no momento do agendamento
  lembrete_enviado BIT NOT NULL DEFAULT 0,
  criado_em        DATETIME(6) NOT NULL,
  FOREIGN KEY (cliente_id)      REFERENCES cliente(id),
  FOREIGN KEY (profissional_id) REFERENCES profissional(id),
  FOREIGN KEY (servico_id)      REFERENCES servico(id),
  INDEX idx_ag_prof_data (profissional_id, data)
);

CREATE TABLE bloqueio_horario (
  id               BIGINT AUTO_INCREMENT PRIMARY KEY,
  profissional_id  BIGINT NOT NULL,
  data             DATE NOT NULL,
  hora_inicio      TIME(6) NOT NULL,
  hora_fim         TIME(6) NOT NULL,
  motivo           VARCHAR(120),
  FOREIGN KEY (profissional_id) REFERENCES profissional(id)
);

CREATE TABLE notificacao (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  usuario_id  BIGINT NOT NULL,
  tipo        VARCHAR(20)  NOT NULL,                 -- NOVO_AGENDAMENTO | LEMBRETE | CANCELAMENTO | REMARCACAO | CONFIRMACAO
  mensagem    VARCHAR(300) NOT NULL,
  lida        BIT NOT NULL DEFAULT 0,
  criada_em   DATETIME(6) NOT NULL,
  FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  INDEX idx_notif_usuario (usuario_id, criada_em)
);
