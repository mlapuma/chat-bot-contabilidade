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

## Deploy em producao

Para producao barata em VPS, use:

```text
docker-compose.prod.yml
PostgreSQL no mesmo servidor
Nginx por IP publico inicialmente
Let's Encrypt depois, quando houver subdominio
Webhook fixo da Meta
```

Guia completo:

```text
docs/deploy-producao-vps.md
```

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

### Assumir atendimento

```bash
curl -X POST http://localhost:8080/api/atendimentos/1/assumir \
  -H "Content-Type: application/json" \
  -d '{"atendente": "Ana"}'
```

### Responder cliente pelo WhatsApp

```bash
curl -X POST http://localhost:8080/api/atendimentos/1/responder \
  -H "Content-Type: application/json" \
  -d '{"atendente": "Ana", "mensagem": "Olá, vou te ajudar com essa solicitação."}'
```

## Fluxo do chatbot

Mensagem inicial:

```text
Olá! Você está falando com a ACSA Contabilidade.
Como podemos ajudar?

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

Na opcao `1 - Abrir empresa`, o bot coleta nome, telefone, e-mail, cidade/estado, atividade principal, tipo de empresa pretendida, se tera socios, se tera funcionarios no inicio e se ja possui CNPJ. Esse fluxo foi desenhado para dar ao contador contexto suficiente para orientar o cliente entre MEI, ME, LTDA ou outro enquadramento adequado.

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

### Teste real com WhatsApp Cloud API

O projeto possui webhook real em:

```text
GET  /api/whatsapp/webhook
POST /api/whatsapp/webhook
```

Variaveis de ambiente:

```text
WHATSAPP_CLOUD_ENABLED=true
WHATSAPP_CLOUD_API_VERSION=v23.0
WHATSAPP_CLOUD_PHONE_NUMBER_ID=seu_phone_number_id
WHATSAPP_CLOUD_ACCESS_TOKEN=seu_token_da_meta
WHATSAPP_CLOUD_VERIFY_TOKEN=um_token_que_voce_escolhe
```

Para testar com o backend local, exponha a porta 8080 com uma URL publica:

```bash
ngrok http 8080
```

No painel da Meta, configure:

```text
Callback URL: https://sua-url-ngrok.ngrok-free.app/api/whatsapp/webhook
Verify token: mesmo valor de WHATSAPP_CLOUD_VERIFY_TOKEN
Webhook field: messages
```

Quando uma mensagem de texto chegar no numero de teste da Meta, o webhook:

- identifica o telefone do remetente;
- cria ou reutiliza o cliente;
- envia a mensagem para `ChatbotService`;
- registra atendimento e historico;
- responde ao usuario usando `/{phone-number-id}/messages`, quando token e phone number id estiverem configurados.

### Painel multiatendente

O painel permite que mais de uma pessoa trabalhe no mesmo número conectado à WhatsApp Cloud API:

- cada atendente informa seu nome no topo do painel;
- a lista mostra quem está responsável por cada atendimento;
- um atendente pode assumir ou liberar um atendimento;
- o histórico mostra mensagens do cliente, do bot e de humanos;
- a resposta humana é registrada no histórico e enviada ao WhatsApp quando a Cloud API estiver configurada.

Observacao: esta versão usa identificação simples pelo nome do atendente no navegador. Para produção, recomenda-se adicionar login, permissões, auditoria e sessões de usuário.

Observacao: a criacao do app, token, numero de teste e assinatura do webhook precisam ser feitas na sua conta Meta, pois dependem de login e permissoes.

Scripts uteis no Windows:

```powershell
.\scripts\start-local-whatsapp-test.ps1 `
  -PhoneNumberId "SEU_PHONE_NUMBER_ID" `
  -AccessToken "SEU_ACCESS_TOKEN"
```

Em outro terminal:

```powershell
ngrok http 8080
```

Para checar se o webhook esta respondendo:

```powershell
.\scripts\check-whatsapp-webhook.ps1
```

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
