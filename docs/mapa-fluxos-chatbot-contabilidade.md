# Mapa de validação dos fluxos do chatbot

Este documento descreve o passo a passo das conversas que o chatbot realiza para cada opção do menu principal. Ele pode ser usado pela equipe da contabilidade para validar se o atendimento inicial está coletando as informações corretas.

## Menu inicial

O bot deve enviar este menu quando o cliente inicia a conversa ou envia uma mensagem de saudação/comando de menu, como:

- `oi`
- `olá`
- `menu`
- `início`
- `opções`
- `bom dia`
- `boa tarde`
- `boa noite`

Mensagem esperada:

```text
Olá! Você está falando com a ACSA Contabilidade.
Como podemos ajudar?

1 - Abrir empresa
2 - Regularizar MEI
3 - Emitir DAS
4 - Imposto de Renda
5 - Folha de pagamento
6 - Certidões negativas
7 - Falar com contador
8 - Enviar documentos
```

Observação: ao enviar uma dessas mensagens de menu, o bot mostra as opções novamente e não deve consumir a mensagem como resposta de um atendimento em andamento.

## Opção 1 - Abrir empresa

Objetivo: coletar os dados iniciais para abertura de empresa.

Tipo registrado: `ABRIR_EMPRESA`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `1` ou texto parecido com "abrir empresa" | `Qual é o seu nome?` | Inicia atendimento |
| 2 | Nome do cliente | `Qual é o seu telefone?` | `nome` |
| 3 | Telefone | `Qual é o seu e-mail?` | `telefone` |
| 4 | E-mail | `Em qual cidade e estado a empresa será aberta?` | `email` |
| 5 | Cidade e estado | `Qual será a atividade principal da empresa?` | `cidadeEstado` |
| 6 | Atividade principal | Menu com tipo de empresa: MEI, ME, LTDA ou ainda não sabe | `atividadePrincipal` |
| 7 | Tipo de empresa pretendida | Menu sobre sócios: sim, não ou ainda não sabe | `tipoEmpresaPretendida` |
| 8 | Informação sobre sócios | Menu sobre funcionários no início: sim, não ou ainda não sabe | `teraSocios` |
| 9 | Informação sobre funcionários | Menu perguntando se já possui CNPJ: sim ou não | `teraFuncionarios` |
| 10 | Sim ou não | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.` | `possuiCnpj` |

Exemplo de conversa:

```text
Cliente: 1
Bot: Qual é o seu nome?
Cliente: Maria Souza
Bot: Qual é o seu telefone?
Cliente: 11 99999-0000
Bot: Qual é o seu e-mail?
Cliente: maria@email.com
Bot: Em qual cidade e estado a empresa será aberta?
Cliente: São Paulo/SP
Bot: Qual será a atividade principal da empresa?
Cliente: Prestação de serviços administrativos
Bot: Qual tipo de empresa você pretende abrir?

1 - MEI
2 - ME
3 - LTDA
4 - Ainda não sei, quero orientação
Cliente: 4
Bot: A empresa terá sócios?

1 - Sim
2 - Não
3 - Ainda não sei
Cliente: 2
Bot: A empresa terá funcionários no início?

1 - Sim
2 - Não
3 - Ainda não sei
Cliente: 3
Bot: Você já possui CNPJ?

1 - Sim
2 - Não
Cliente: Não
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.
```

Critério de validação: o atendimento deve ficar registrado como `NOVO` e disponível para análise do escritório, contendo nome, telefone, e-mail, cidade/estado, atividade principal, tipo de empresa pretendida, informação sobre sócios, informação sobre funcionários e se já possui CNPJ.

## Opção 2 - Regularizar MEI

Objetivo: coletar dados para regularização de MEI.

Tipo registrado: `REGULARIZAR_MEI`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `2` ou texto parecido com "regularizar MEI" | `Informe o CNPJ do MEI.` | Inicia atendimento |
| 2 | CNPJ do MEI | Menu com a situação atual do MEI: ativo com DAS em atraso, ativo com dados a regularizar, baixado/cancelado, inapto/pendente, desenquadrado ou não sabe | `cnpj` |
| 3 | Situação atual | Menu sobre DAS em atraso: sim, não ou não sei | `situacaoAtual` |
| 4 | Informação sobre DAS em atraso | `Qual telefone para contato?` | `dasAtrasado` |
| 5 | Telefone | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.` | `telefone` |

Exemplo de conversa:

```text
Cliente: 2
Bot: Informe o CNPJ do MEI.
Cliente: 12.345.678/0001-90
Bot: Qual é a situação atual do MEI?

1 - Está ativo, mas tenho DAS em atraso
2 - Está ativo, mas preciso regularizar dados
3 - Está baixado/cancelado
4 - Está inapto ou com pendências
5 - Fui desenquadrado do MEI
6 - Não sei a situação atual
Cliente: 4
Bot: Há DAS em atraso?

1 - Sim
2 - Não
3 - Não sei
Cliente: 1
Bot: Qual telefone para contato?
Cliente: 11 98888-7777
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.
```

Critério de validação: o atendimento deve conter CNPJ, situação atual, informação sobre DAS em atraso e telefone. O cliente deve conseguir responder usando o número da opção ou escrevendo a situação em texto livre.

## Opção 3 - Emitir DAS

Objetivo: coletar informações para emissão do DAS.

Tipo registrado: `EMITIR_DAS`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `3` ou texto parecido com "emitir DAS" | `Informe o CNPJ.` | Inicia atendimento |
| 2 | CNPJ | `Qual é o mês de referência?` | `cnpj` |
| 3 | Mês de referência | `Qual é o e-mail que devemos usar para envio?` | `mesReferencia` |
| 4 | E-mail | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento. O PDF interno da solicitação do DAS foi gerado para uso da equipe contábil. O contador fará a emissão oficial e entrará em contato com você.` | `emailEnvio`, `modoEmissaoDas`, `statusEmissaoDas`, `arquivoDasPdf`, `urlDasPdf` |

Exemplo de conversa:

```text
Cliente: 3
Bot: Informe o CNPJ.
Cliente: 12.345.678/0001-90
Bot: Qual é o mês de referência?
Cliente: Junho/2026
Bot: Qual é o e-mail que devemos usar para envio?
Cliente: financeiro@empresa.com.br
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.

O PDF interno da solicitação do DAS foi gerado para uso da equipe contábil. O contador fará a emissão oficial e entrará em contato com você.
```

Critério de validação: o atendimento deve conter CNPJ, mês de referência, e-mail de envio, caminho do PDF gerado e URL interna para download. O campo `modoEmissaoDas` deve ficar como `MANUAL_ASSISTIDO` e o campo `statusEmissaoDas` deve ficar como `AGUARDANDO_EMISSAO_OFICIAL`. O PDF é um comprovante interno da solicitação de emissão do DAS; a emissão fiscal real deve ser confirmada pelo contador nos sistemas oficiais. A URL interna `/api/atendimentos/{id}/das-pdf` não deve aparecer para o cliente no WhatsApp.

## Opção 4 - Imposto de Renda

Objetivo: coletar dados para atendimento de Imposto de Renda.

Tipo registrado: `IMPOSTO_DE_RENDA`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `4` ou texto parecido com "Imposto de Renda" | `Qual é o seu nome?` | Inicia atendimento |
| 2 | Nome | `Informe o CPF.` | `nome` |
| 3 | CPF | `Qual é o ano de referência?` | `cpf` |
| 4 | Ano de referência | `Você possui informes de rendimento?` | `anoReferencia` |
| 5 | Sim ou não | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.` | `possuiInformesRendimento` |

Exemplo de conversa:

```text
Cliente: 4
Bot: Qual é o seu nome?
Cliente: João Pereira
Bot: Informe o CPF.
Cliente: 123.456.789-00
Bot: Qual é o ano de referência?
Cliente: 2026
Bot: Você possui informes de rendimento?
Cliente: Sim
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.
```

Critério de validação: o atendimento deve conter nome, CPF, ano de referência e confirmação sobre informes de rendimento.

## Opção 5 - Folha de pagamento

Objetivo: coletar dados para atendimento de folha de pagamento.

Tipo registrado: `FOLHA_DE_PAGAMENTO`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `5` ou texto parecido com "folha de pagamento" | `Informe o CNPJ.` | Inicia atendimento |
| 2 | CNPJ | `Quantos funcionários a empresa possui?` | `cnpj` |
| 3 | Quantidade de funcionários | `Qual é o mês de competência?` | `quantidadeFuncionarios` |
| 4 | Mês de competência | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.` | `mesCompetencia` |

Exemplo de conversa:

```text
Cliente: 5
Bot: Informe o CNPJ.
Cliente: 12.345.678/0001-90
Bot: Quantos funcionários a empresa possui?
Cliente: 8
Bot: Qual é o mês de competência?
Cliente: Junho/2026
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.
```

Critério de validação: o atendimento deve conter CNPJ, quantidade de funcionários e mês de competência.

## Opção 6 - Certidões negativas

Objetivo: coletar dados para emissão ou consulta de certidões negativas.

Tipo registrado: `CERTIDOES_NEGATIVAS`

Status durante coleta: `AGUARDANDO_CLIENTE`

Status ao concluir: `NOVO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `6` ou texto parecido com "certidões negativas" | `Informe o CNPJ.` | Inicia atendimento |
| 2 | CNPJ | Menu com tipos de certidão: Receita Federal/PGFN, FGTS, Trabalhista, Estadual, Municipal, Falência/recuperação judicial, todas ou ajuda do contador | `cnpj` |
| 3 | Tipo de certidão | Menu com finalidades: licitação, conta bancária, financiamento, fornecedor, regularização ou outra finalidade | `tipoCertidao` |
| 4 | Finalidade | `Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.` | `finalidade` |

Exemplo de conversa:

```text
Cliente: 6
Bot: Informe o CNPJ.
Cliente: 12.345.678/0001-90
Bot: Qual tipo de certidão você precisa?

1 - Receita Federal / PGFN
2 - FGTS
3 - Trabalhista
4 - Estadual
5 - Municipal
6 - Falência e recuperação judicial
7 - Todas as certidões
8 - Não sei, quero ajuda do contador
Cliente: 1
Bot: Qual é a finalidade da certidão?

Exemplos:
1 - Participar de licitação
2 - Abrir conta bancária
3 - Financiamento
4 - Cadastro em fornecedor
5 - Regularização da empresa
6 - Outra finalidade
Cliente: 1
Bot: Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.
```

Critério de validação: o atendimento deve conter CNPJ, tipo de certidão escolhido e finalidade. O cliente deve conseguir responder usando o número da opção ou escrevendo a certidão/finalidade em texto livre.

## Opção 7 - Falar com contador

Objetivo: encaminhar o cliente para atendimento humano.

Tipo registrado: `FALAR_COM_CONTADOR`

Status final: `AGUARDANDO_HUMANO`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `7` ou texto parecido com "falar com contador" | `Certo. Seu atendimento foi encaminhado para um contador. Em breve, nossa equipe entrará em contato.` | Cria atendimento sem coleta adicional |

Exemplo de conversa:

```text
Cliente: 7
Bot: Certo. Seu atendimento foi encaminhado para um contador. Em breve, nossa equipe entrará em contato.
```

Critério de validação: o atendimento deve ser criado com status `AGUARDANDO_HUMANO`.

## Opção 8 - Enviar documentos

Objetivo: orientar o cliente sobre envio de documentos.

Tipo registrado: `ENVIAR_DOCUMENTOS`

Status final: `DOCUMENTOS_PENDENTES`

| Passo | Cliente envia | Bot responde | Campo registrado |
| --- | --- | --- | --- |
| 1 | `8` ou texto parecido com "enviar documentos" | `Você pode enviar os documentos pelo canal combinado com o escritório. Caso esteja no WhatsApp, anexe os arquivos nesta conversa. Seu atendimento ficou como documentos pendentes.` | Cria atendimento sem coleta adicional |

Exemplo de conversa:

```text
Cliente: 8
Bot: Você pode enviar os documentos pelo canal combinado com o escritório. Caso esteja no WhatsApp, anexe os arquivos nesta conversa. Seu atendimento ficou como documentos pendentes.
```

Critério de validação: o atendimento deve ser criado com status `DOCUMENTOS_PENDENTES`.

## Validação rápida pela contabilidade

Use este checklist para homologar o fluxo:

- Enviar `oi` e confirmar se o menu de 1 a 8 aparece.
- Testar cada opção digitando apenas o número.
- Confirmar se as perguntas aparecem na ordem descrita neste documento.
- Confirmar se, ao final das opções 1 a 6, o bot informa que a solicitação foi registrada.
- Confirmar se a opção 7 fica como `AGUARDANDO_HUMANO`.
- Confirmar se a opção 8 fica como `DOCUMENTOS_PENDENTES`.
- Durante qualquer atendimento aberto, enviar `menu` ou `oi` e confirmar se o bot mostra o menu novamente.
- No painel do escritório, validar se os atendimentos aparecem na listagem e se o histórico da conversa está disponível.
