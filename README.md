# Chatbot para Escritorio de Contabilidade

Projeto completo de atendimento inicial para escritorio contabil, com API REST em Java 17 + Spring Boot, PostgreSQL, frontend simples e estrutura preparada para futura integracao com WhatsApp Business API.

## Tecnologias

- Java 17
- Spring Boot 3
- Spring Web, Spring Data JPA e Bean Validation
- PostgreSQL
- Flyway
- HTML, CSS e JavaScript
- Docker Compose
- JUnit 5 e Mockito

## Como rodar com Docker

```bash
docker compose up --build
```

Acesse:

- Frontend: `http://localhost:8080`
- API: `http://localhost:8080/api`
- PostgreSQL: `localhost:5432`

Credenciais padrao do banco:

- Database: `chatbot_contabilidade`
- Usuario: `chatbot`
- Senha: `chatbot123`

## Endpoints principais

### Criar cliente

```bash
curl -X POST http://localhost:8080/api/clientes \
  -H "Content-Type: application/json" \
  -d '{
    "nome": "Maria Silva",
    "telefone": "11999990000",
    "email": "maria@email.com",
    "cpfCnpj": "12345678901",
    "tipoPessoa": "FISICA",
    "status": "ATIVO"
  }'
```

### Listar clientes

```bash
curl http://localhost:8080/api/clientes
```

### Enviar mensagem ao chatbot

```bash
curl -X POST http://localhost:8080/api/chat/enviar \
  -H "Content-Type: application/json" \
  -d '{"clienteId": 1, "mensagem": "1"}'
```

### Ver historico do cliente

```bash
curl http://localhost:8080/api/chat/historico/1
```

### Listar atendimentos

```bash
curl http://localhost:8080/api/atendimentos
```

### Buscar atendimento

```bash
curl http://localhost:8080/api/atendimentos/1
```

### Atualizar status

```bash
curl -X PUT http://localhost:8080/api/atendimentos/1/status \
  -H "Content-Type: application/json" \
  -d '{"status": "FINALIZADO"}'
```

## Fluxo do chatbot

Mensagem inicial:

```text
Ola! Seja bem-vindo ao atendimento do escritorio contabil. Como posso ajudar?

1 - Abrir empresa
2 - Regularizar MEI
3 - Emitir DAS
4 - Imposto de Renda
5 - Folha de pagamento
6 - Certidoes negativas
7 - Falar com contador
8 - Enviar documentos
```

Cada opcao cria um `Atendimento`, registra as mensagens em `MensagemChat`, coleta os campos necessarios e salva os dados em JSON no campo `dadosColetados`.

Status disponiveis:

- `NOVO`
- `EM_ANDAMENTO`
- `AGUARDANDO_CLIENTE`
- `AGUARDANDO_HUMANO`
- `DOCUMENTOS_PENDENTES`
- `FINALIZADO`

## Arquitetura

```text
controller -> service -> repository -> entity
```

Pacotes principais:

- `controller`: endpoints REST
- `service`: regras de negocio e fluxo do chatbot
- `repository`: acesso ao banco
- `dto`: objetos de entrada e saida da API
- `entity`: entidades JPA e enums
- `exception`: tratamento global com `@RestControllerAdvice`

## WhatsApp Business API

A interface `CanalMensagemService` permite trocar o canal de envio de mensagens. A implementacao `WebChatService` cobre o chat web atual. A classe `WhatsAppService` ja esta preparada com comentarios indicando os pontos de integracao com a WhatsApp Business Cloud API.

## Testes

Para executar os testes:

```bash
mvn test
```

Os testes cobrem:

- `ChatbotService`
- `ClienteService`
- `AtendimentoService`

## Melhorias futuras

- Autenticacao para o painel do escritorio.
- Filtros por status e tipo de atendimento.
- Upload real de documentos.
- Criacao automatica de cliente a partir do WhatsApp.
- Webhook completo da WhatsApp Business Cloud API.
- Notificacoes internas para contadores.
- Mascaramento adicional de CPF/CNPJ nos logs e respostas administrativas.
