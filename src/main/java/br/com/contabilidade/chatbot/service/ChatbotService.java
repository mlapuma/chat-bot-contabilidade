package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.MensagemChatDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.DirecaoMensagem;
import br.com.contabilidade.chatbot.entity.MensagemChat;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import br.com.contabilidade.chatbot.repository.MensagemChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotService.class);
    private static final List<StatusAtendimento> STATUS_ABERTOS = List.of(
            StatusAtendimento.EM_ANDAMENTO,
            StatusAtendimento.AGUARDANDO_CLIENTE
    );
    private static final String NOME_CONTABILIDADE = "ACSA Contabilidade";
    private static final String ESTADO_TRIAGEM = "estadoTriagem";
    private static final String ESTADO_CLIENTE_DEPARTAMENTO = "cliente_departamento";
    private static final String ESTADO_NOVOS_CLIENTES = "novos_clientes_menu";
    private static final String ESTADO_VERIFICAR_CLIENTE = "verificar_cliente";
    private static final String ESTADO_DP_ASSUNTO = "dp_assunto";
    private static final String ESTADO_DP_FOLHA_ASSUNTO = "dp_folha_assunto";
    private static final String ESTADO_DP_COLETA = "dp_coleta";
    private static final String ROTEIRO_COLETA = "roteiroColeta";
    private static final String INDICE_COLETA = "indiceColeta";
    private static final String ROTEIRO_DP_FOLHA = "DP_FOLHA";
    private static final String ROTEIRO_DP_PRO_LABORE = "DP_PRO_LABORE";
    private static final String ROTEIRO_DP_RECALCULO_GUIA = "DP_RECALCULO_GUIA";
    private static final String ROTEIRO_DP_INSS = "DP_INSS";
    private static final String ROTEIRO_DP_FGTS = "DP_FGTS";
    private static final String ROTEIRO_DP_RESPONSAVEL = "DP_RESPONSAVEL";

    private final AtendimentoRepository atendimentoRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final ClienteService clienteService;
    private final ObjectMapper objectMapper;
    private final DasEmissionService dasEmissionService;

    public ChatbotService(AtendimentoRepository atendimentoRepository,
                          MensagemChatRepository mensagemChatRepository,
                          ClienteService clienteService,
                          ObjectMapper objectMapper,
                          DasEmissionService dasEmissionService) {
        this.atendimentoRepository = atendimentoRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.clienteService = clienteService;
        this.objectMapper = objectMapper;
        this.dasEmissionService = dasEmissionService;
    }

    @Transactional
    public ChatResponseDTO enviar(ChatRequestDTO request) {
        Cliente cliente = request.getClienteId() == null ? null : clienteService.buscarEntidade(request.getClienteId());
        String texto = request.getMensagem() == null ? "" : request.getMensagem().trim();

        if (deveEnviarMenu(texto)) {
            return responderComMenuInicial(cliente, texto);
        }

        Optional<Atendimento> atendimentoAberto = cliente == null
                ? Optional.empty()
                : atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(cliente.getId(), STATUS_ABERTOS);

        if (atendimentoAberto.isPresent()) {
            return continuarAtendimento(atendimentoAberto.get(), cliente, request);
        }

        if (isRespostaTriagemInicial(texto)) {
            return iniciarTriagemInicial(cliente, request);
        }

        TipoAtendimento tipo = identificarIntencao(texto);
        if (tipo == null) {
            String resposta = mensagemInicialTriagem();
            salvarMensagem(cliente, null, DirecaoMensagem.CLIENTE, texto);
            salvarMensagem(cliente, null, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, null, null, null);
        }

        Atendimento atendimento = criarAtendimento(cliente, tipo, request);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        if (tipo == TipoAtendimento.FALAR_COM_CONTADOR) {
            atendimento.setStatus(StatusAtendimento.AGUARDANDO_HUMANO);
            Map<String, String> dados = lerDados(atendimento.getDadosColetados());
            dados.put("assunto", "Falar com contador");
            atendimento.setDadosColetados(escreverDados(dados));
            atendimentoRepository.save(atendimento);
            String resposta = "Certo. Seu atendimento foi encaminhado para um contador. Em breve, nossa equipe entrará em contato.";
            salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, atendimento.getId(), tipo, atendimento.getStatus());
        }

        if (tipo == TipoAtendimento.ENVIAR_DOCUMENTOS) {
            atendimento.setStatus(StatusAtendimento.DOCUMENTOS_PENDENTES);
            Map<String, String> dados = lerDados(atendimento.getDadosColetados());
            dados.put("assunto", "Enviar documentos");
            atendimento.setDadosColetados(escreverDados(dados));
            atendimentoRepository.save(atendimento);
            String resposta = "Você pode enviar os documentos pelo canal combinado com o escritório. Caso esteja no WhatsApp, anexe os arquivos nesta conversa. Seu atendimento ficou como documentos pendentes.";
            salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, atendimento.getId(), tipo, atendimento.getStatus());
        }

        String primeiraPergunta = perguntas(tipo).get(0);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, primeiraPergunta);
        return new ChatResponseDTO(primeiraPergunta, atendimento.getId(), tipo, atendimento.getStatus());
    }

    @Transactional
    public ChatResponseDTO enviarMenuInicial(Long clienteId, String mensagemRecebida) {
        Cliente cliente = clienteId == null ? null : clienteService.buscarEntidade(clienteId);
        String texto = mensagemRecebida == null ? "" : mensagemRecebida.trim();
        return responderComMenuInicial(cliente, texto);
    }

    private ChatResponseDTO responderComMenuInicial(Cliente cliente, String texto) {
        String resposta = mensagemInicialTriagem();

        if (!texto.isBlank()) {
            salvarMensagem(cliente, null, DirecaoMensagem.CLIENTE, texto);
        }
        encerrarAtendimentosEmColeta(cliente);
        salvarMensagem(cliente, null, DirecaoMensagem.BOT, resposta);
        return new ChatResponseDTO(resposta, null, null, null);
    }

    private void encerrarAtendimentosEmColeta(Cliente cliente) {
        if (cliente == null || cliente.getId() == null) {
            return;
        }

        List<Atendimento> atendimentos = atendimentoRepository.findByClienteIdAndStatusIn(cliente.getId(), STATUS_ABERTOS);
        if (atendimentos == null || atendimentos.isEmpty()) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        atendimentos.forEach(atendimento -> {
            atendimento.setStatus(StatusAtendimento.FINALIZADO);
            atendimento.setAtualizadoEm(agora);
        });
        atendimentoRepository.saveAll(atendimentos);
    }

    @Transactional(readOnly = true)
    public List<MensagemChatDTO> historico(Long clienteId) {
        return mensagemChatRepository.findByClienteIdOrderByCriadoEmAsc(clienteId).stream()
                .map(this::toDto)
                .toList();
    }

    private ChatResponseDTO continuarAtendimento(Atendimento atendimento, Cliente cliente, ChatRequestDTO request) {
        String texto = request.getMensagem() == null ? "" : request.getMensagem().trim();
        if (atendimento.getTipoAtendimento() == TipoAtendimento.TRIAGEM_INICIAL
                || ESTADO_DP_COLETA.equals(lerDados(atendimento.getDadosColetados()).get(ESTADO_TRIAGEM))) {
            return continuarTriagemInicial(atendimento, cliente, request);
        }

        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
        aplicarOrigemWhatsApp(request, dados);
        List<String> campos = campos(atendimento.getTipoAtendimento());
        int etapa = atendimento.getEtapaAtual();
        if (etapa < campos.size()) {
            dados.put(campos.get(etapa), texto);
        }

        etapa++;
        atendimento.setEtapaAtual(etapa);
        atendimento.setDadosColetados(escreverDados(dados));
        atendimento.setAtualizadoEm(LocalDateTime.now());

        String resposta;
        if (etapa >= campos.size()) {
            atendimento.setStatus(StatusAtendimento.NOVO);
            resposta = "Obrigado. Registrei sua solicitação e um contador entrará em contato para dar continuidade ao atendimento.";
            if (atendimento.getTipoAtendimento() == TipoAtendimento.EMITIR_DAS) {
                DasEmissionResult emissaoDas = dasEmissionService.emitir(new DasEmissionRequest(atendimento, dados));
                dados.put("modoEmissaoDas", emissaoDas.modoEmissao());
                dados.put("statusEmissaoDas", emissaoDas.statusEmissao());
                dados.put("arquivoDasPdf", emissaoDas.arquivoPdf());
                dados.put("urlDasPdf", emissaoDas.urlPdf());
                atendimento.setDadosColetados(escreverDados(dados));
                resposta = resposta + "\n\n" + emissaoDas.mensagemCliente();
            }
            log.info("Atendimento {} registrado com tipo={}", atendimento.getId(), atendimento.getTipoAtendimento());
        } else {
            atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
            resposta = perguntas(atendimento.getTipoAtendimento()).get(etapa);
        }

        atendimentoRepository.save(atendimento);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
        return new ChatResponseDTO(resposta, atendimento.getId(), atendimento.getTipoAtendimento(), atendimento.getStatus());
    }

    private ChatResponseDTO iniciarTriagemInicial(Cliente cliente, ChatRequestDTO request) {
        String texto = request.getMensagem() == null ? "" : request.getMensagem().trim();
        Atendimento atendimento = criarAtendimento(cliente, TipoAtendimento.TRIAGEM_INICIAL, request);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        Map<String, String> dados = new LinkedHashMap<>();
        aplicarOrigemWhatsApp(request, dados);
        String resposta = switch (normalizarTexto(texto)) {
            case "1" -> {
                dados.put("perfilCliente", "JA_CLIENTE");
                dados.put(ESTADO_TRIAGEM, ESTADO_CLIENTE_DEPARTAMENTO);
                yield menuDepartamentosCliente();
            }
            case "2" -> {
                dados.put("perfilCliente", "NAO_CLIENTE");
                dados.put(ESTADO_TRIAGEM, ESTADO_NOVOS_CLIENTES);
                yield menuNovosClientes();
            }
            default -> {
                dados.put("perfilCliente", "NAO_SEI");
                dados.put(ESTADO_TRIAGEM, ESTADO_VERIFICAR_CLIENTE);
                yield "Sem problema. Informe seu CPF ou CNPJ para verificarmos seu cadastro e direcionarmos o atendimento.";
            }
        };

        atendimento.setDadosColetados(escreverDados(dados));
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        atendimentoRepository.save(atendimento);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
        return new ChatResponseDTO(resposta, atendimento.getId(), atendimento.getTipoAtendimento(), atendimento.getStatus());
    }

    private ChatResponseDTO continuarTriagemInicial(Atendimento atendimento, Cliente cliente, ChatRequestDTO request) {
        String texto = request.getMensagem() == null ? "" : request.getMensagem().trim();
        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
        aplicarOrigemWhatsApp(request, dados);
        String estado = dados.getOrDefault(ESTADO_TRIAGEM, ESTADO_CLIENTE_DEPARTAMENTO);
        String normalizado = normalizarTexto(texto);
        String resposta;

        switch (estado) {
            case ESTADO_CLIENTE_DEPARTAMENTO -> resposta = tratarDepartamentoCliente(atendimento, dados, normalizado);
            case ESTADO_DP_ASSUNTO -> resposta = tratarAssuntoDepartamentoPessoal(atendimento, dados, normalizado);
            case ESTADO_DP_FOLHA_ASSUNTO -> resposta = tratarAssuntoFolhaPagamento(atendimento, dados, normalizado);
            case ESTADO_DP_COLETA -> resposta = continuarColetaDepartamentoPessoal(atendimento, dados, texto);
            case ESTADO_NOVOS_CLIENTES -> {
                TipoAtendimento tipo = identificarIntencao(texto);
                if (tipo == null || tipo == TipoAtendimento.TRIAGEM_INICIAL) {
                    resposta = "Não consegui identificar a opção. Escolha uma das opções abaixo:\n\n" + menuNovosClientes();
                } else {
                    resposta = converterTriagemParaAtendimento(atendimento, dados, tipo);
                }
            }
            case ESTADO_VERIFICAR_CLIENTE -> {
                dados.put("cpfCnpjInformado", texto);
                atendimento.setTipoAtendimento(TipoAtendimento.CLIENTE_VERIFICACAO);
                atendimento.setStatus(StatusAtendimento.AGUARDANDO_HUMANO);
                resposta = "Obrigado. Vamos verificar seu cadastro e um responsável da ACSA Contabilidade dará continuidade ao atendimento.";
            }
            default -> {
                dados.put(ESTADO_TRIAGEM, ESTADO_CLIENTE_DEPARTAMENTO);
                resposta = menuDepartamentosCliente();
            }
        }

        atendimento.setDadosColetados(escreverDados(dados));
        atendimento.setAtualizadoEm(LocalDateTime.now());
        atendimentoRepository.save(atendimento);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
        return new ChatResponseDTO(resposta, atendimento.getId(), atendimento.getTipoAtendimento(), atendimento.getStatus());
    }

    private String tratarDepartamentoCliente(Atendimento atendimento, Map<String, String> dados, String normalizado) {
        return switch (normalizado) {
            case "1" -> {
                dados.put("departamento", "Departamento Pessoal");
                dados.put(ESTADO_TRIAGEM, ESTADO_DP_ASSUNTO);
                yield menuDepartamentoPessoal();
            }
            case "2" -> registrarSolicitacaoInterna(
                    atendimento,
                    dados,
                    TipoAtendimento.CLIENTE_FISCAL,
                    "Fiscal",
                    "Atendimento fiscal"
            );
            case "3" -> registrarSolicitacaoInterna(
                    atendimento,
                    dados,
                    TipoAtendimento.CLIENTE_PARALEGAL,
                    "Jurídico",
                    "Atendimento jurídico"
            );
            case "4" -> {
                dados.put("departamento", "Contabilidade");
                dados.put("assunto", "Falar com contador");
                atendimento.setTipoAtendimento(TipoAtendimento.FALAR_COM_CONTADOR);
                atendimento.setStatus(StatusAtendimento.AGUARDANDO_HUMANO);
                yield "Certo. Encaminhei seu atendimento para um contador da ACSA Contabilidade.";
            }
            default -> "Não consegui identificar o departamento. Escolha uma das opções abaixo:\n\n" + menuDepartamentosCliente();
        };
    }

    private String tratarAssuntoDepartamentoPessoal(Atendimento atendimento, Map<String, String> dados, String normalizado) {
        return switch (normalizado) {
            case "1" -> {
                dados.put("assuntoDepartamentoPessoal", "Folha de pagamento");
                dados.put(ESTADO_TRIAGEM, ESTADO_DP_FOLHA_ASSUNTO);
                yield menuFolhaPagamento();
            }
            case "2" -> iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_PRO_LABORE, "Pró-labore");
            case "3" -> iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_RECALCULO_GUIA, "Recálculo de guia");
            case "4" -> iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_INSS, "INSS");
            case "5" -> iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_FGTS, "FGTS");
            case "6" -> iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_RESPONSAVEL, "Falar com responsável do Departamento Pessoal");
            case "7" -> {
                dados.put(ESTADO_TRIAGEM, ESTADO_CLIENTE_DEPARTAMENTO);
                yield menuDepartamentosCliente();
            }
            default -> "Não consegui identificar o assunto. Escolha uma das opções abaixo:\n\n" + menuDepartamentoPessoal();
        };
    }

    private String tratarAssuntoFolhaPagamento(Atendimento atendimento, Map<String, String> dados, String normalizado) {
        String assunto = switch (normalizado) {
            case "1" -> "Férias";
            case "2" -> "Rescisão";
            case "3" -> "Admissão";
            case "4" -> "Dúvida sobre folha mensal";
            case "5" -> "Envio de documentos da folha";
            case "6" -> "Outro assunto de folha de pagamento";
            default -> null;
        };

        if (assunto == null) {
            return "Não consegui identificar o assunto de folha. Escolha uma das opções abaixo:\n\n" + menuFolhaPagamento();
        }

        dados.put("departamento", "Departamento Pessoal");
        dados.put("assuntoDepartamentoPessoal", "Folha de pagamento");
        dados.put("assuntoFolhaPagamento", assunto);
        return iniciarColetaDepartamentoPessoal(atendimento, dados, ROTEIRO_DP_FOLHA, "Folha de pagamento - " + assunto);
    }

    private String iniciarColetaDepartamentoPessoal(Atendimento atendimento,
                                                    Map<String, String> dados,
                                                    String roteiro,
                                                    String assunto) {
        dados.put("departamento", "Departamento Pessoal");
        dados.put("assunto", assunto);
        dados.put(ROTEIRO_COLETA, roteiro);
        dados.put(INDICE_COLETA, "0");
        dados.put(ESTADO_TRIAGEM, ESTADO_DP_COLETA);
        atendimento.setTipoAtendimento(tipoFinalDepartamentoPessoal(roteiro));
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        return primeiraPerguntaColeta(roteiro);
    }

    private String continuarColetaDepartamentoPessoal(Atendimento atendimento, Map<String, String> dados, String texto) {
        String roteiro = dados.getOrDefault(ROTEIRO_COLETA, ROTEIRO_DP_RESPONSAVEL);
        List<CampoPergunta> perguntas = perguntasColetaDepartamentoPessoal(roteiro);
        int indice = parseIndiceColeta(dados.get(INDICE_COLETA));

        if (indice < perguntas.size()) {
            dados.put(perguntas.get(indice).campo(), texto);
        }

        indice++;
        dados.put(INDICE_COLETA, String.valueOf(indice));

        if (indice >= perguntas.size()) {
            atendimento.setTipoAtendimento(tipoFinalDepartamentoPessoal(roteiro));
            atendimento.setStatus(ROTEIRO_DP_RESPONSAVEL.equals(roteiro)
                    ? StatusAtendimento.AGUARDANDO_HUMANO
                    : StatusAtendimento.NOVO);
            return "Obrigado. Reuni as informações necessárias e registrei sua solicitação para o Departamento Pessoal. Nossa equipe dará continuidade ao atendimento.";
        }

        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        return perguntas.get(indice).pergunta();
    }

    private String primeiraPerguntaColeta(String roteiro) {
        return perguntasColetaDepartamentoPessoal(roteiro).get(0).pergunta();
    }

    private int parseIndiceColeta(String valor) {
        try {
            return valor == null ? 0 : Integer.parseInt(valor);
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private TipoAtendimento tipoFinalDepartamentoPessoal(String roteiro) {
        return ROTEIRO_DP_FOLHA.equals(roteiro)
                ? TipoAtendimento.FOLHA_DE_PAGAMENTO
                : TipoAtendimento.CLIENTE_DEPARTAMENTO_PESSOAL;
    }

    private List<CampoPergunta> perguntasColetaDepartamentoPessoal(String roteiro) {
        return switch (roteiro) {
            case ROTEIRO_DP_FOLHA -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("mesCompetencia", "Qual é o mês de competência da folha?"),
                    new CampoPergunta("quantidadeFuncionarios", "Quantos funcionários a empresa possui atualmente?"),
                    new CampoPergunta("descricaoSolicitacao", "Descreva rapidamente o que precisa ser tratado nesse assunto.")
            );
            case ROTEIRO_DP_PRO_LABORE -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("nomeSocio", "Informe o nome do sócio."),
                    new CampoPergunta("cpfSocio", "Informe o CPF do sócio."),
                    new CampoPergunta("valorProLabore", "Qual é o valor do pró-labore?"),
                    new CampoPergunta("mesReferencia", "Qual é o mês de referência?"),
                    new CampoPergunta("tipoSolicitacao", "A solicitação é inclusão, alteração, suspensão ou dúvida?")
            );
            case ROTEIRO_DP_RECALCULO_GUIA -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("tipoGuia", "Qual guia precisa ser recalculada? Exemplo: INSS, FGTS, IRRF, DAE, eSocial ou outra."),
                    new CampoPergunta("mesCompetencia", "Qual é o mês de competência da guia?"),
                    new CampoPergunta("dataPagamento", "Qual é a nova data desejada para pagamento?"),
                    new CampoPergunta("motivoRecalculo", "Qual é o motivo do recálculo? Exemplo: guia vencida, valor incorreto, perda da guia ou alteração na folha.")
            );
            case ROTEIRO_DP_INSS -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("mesCompetencia", "Qual é o mês de competência?"),
                    new CampoPergunta("tipoSolicitacao", "O assunto é guia, desconto, afastamento, pró-labore, funcionário ou outro?"),
                    new CampoPergunta("envolvido", "Se houver, informe o nome do funcionário ou sócio envolvido."),
                    new CampoPergunta("descricaoSolicitacao", "Descreva rapidamente a dúvida ou problema com INSS.")
            );
            case ROTEIRO_DP_FGTS -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("mesCompetencia", "Qual é o mês de competência?"),
                    new CampoPergunta("funcionario", "Se for um caso individual, informe o nome do funcionário. Se não for, digite 'geral'."),
                    new CampoPergunta("tipoSolicitacao", "O assunto é guia, extrato, diferença, rescisão, atraso ou regularização?"),
                    new CampoPergunta("dataPagamento", "Se precisar emitir guia, qual é a data prevista de pagamento?")
            );
            case ROTEIRO_DP_RESPONSAVEL -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("nomeSolicitante", "Informe seu nome."),
                    new CampoPergunta("contatoRetorno", "Informe o telefone ou e-mail para retorno."),
                    new CampoPergunta("assuntoResumido", "Resuma o assunto que deseja tratar com o responsável do Departamento Pessoal."),
                    new CampoPergunta("urgencia", "Qual é a urgência? Responda normal ou urgente.")
            );
            default -> List.of(
                    new CampoPergunta("cnpj", "Informe o CNPJ da empresa."),
                    new CampoPergunta("descricaoSolicitacao", "Descreva rapidamente sua solicitação para o Departamento Pessoal.")
            );
        };
    }

    private String registrarSolicitacaoInterna(Atendimento atendimento,
                                               Map<String, String> dados,
                                               TipoAtendimento tipo,
                                               String departamento,
                                               String assunto) {
        dados.put("departamento", departamento);
        dados.put("assunto", assunto);
        atendimento.setTipoAtendimento(tipo);
        atendimento.setStatus(StatusAtendimento.NOVO);
        return "Obrigado. Registrei sua solicitação para o departamento " + departamento + ". Nossa equipe dará continuidade ao atendimento.";
    }

    private String converterTriagemParaAtendimento(Atendimento atendimento, Map<String, String> dados, TipoAtendimento tipo) {
        atendimento.setTipoAtendimento(tipo);
        atendimento.setEtapaAtual(0);

        if (tipo == TipoAtendimento.FALAR_COM_CONTADOR) {
            limparDadosPreservandoOrigem(dados);
            dados.put("assunto", "Falar com contador");
            atendimento.setStatus(StatusAtendimento.AGUARDANDO_HUMANO);
            return "Certo. Seu atendimento foi encaminhado para um contador. Em breve, nossa equipe entrará em contato.";
        }

        if (tipo == TipoAtendimento.ENVIAR_DOCUMENTOS) {
            limparDadosPreservandoOrigem(dados);
            dados.put("assunto", "Enviar documentos");
            atendimento.setStatus(StatusAtendimento.DOCUMENTOS_PENDENTES);
            return "Você pode anexar os documentos nesta conversa. Seu atendimento ficou como documentos pendentes.";
        }

        limparDadosPreservandoOrigem(dados);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        List<String> perguntas = perguntas(tipo);
        return perguntas.isEmpty()
                ? "Obrigado. Registrei sua solicitação e nossa equipe dará continuidade ao atendimento."
                : perguntas.get(0);
    }

    private void limparDadosPreservandoOrigem(Map<String, String> dados) {
        String numeroDestino = dados.get("numeroWhatsAppDestino");
        String phoneNumberIdDestino = dados.get("phoneNumberIdDestino");
        dados.clear();
        if (numeroDestino != null && !numeroDestino.isBlank()) {
            dados.put("numeroWhatsAppDestino", numeroDestino);
        }
        if (phoneNumberIdDestino != null && !phoneNumberIdDestino.isBlank()) {
            dados.put("phoneNumberIdDestino", phoneNumberIdDestino);
        }
    }

    private Atendimento criarAtendimento(Cliente cliente, TipoAtendimento tipo, ChatRequestDTO request) {
        Atendimento atendimento = new Atendimento();
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(tipo);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(0);
        Map<String, String> dados = new LinkedHashMap<>();
        aplicarOrigemWhatsApp(request, dados);
        atendimento.setDadosColetados(escreverDados(dados));
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return atendimentoRepository.save(atendimento);
    }

    private void aplicarOrigemWhatsApp(ChatRequestDTO request, Map<String, String> dados) {
        if (request == null) {
            return;
        }
        if (request.getNumeroWhatsAppDestino() != null && !request.getNumeroWhatsAppDestino().isBlank()) {
            dados.put("numeroWhatsAppDestino", request.getNumeroWhatsAppDestino());
        }
        if (request.getPhoneNumberIdDestino() != null && !request.getPhoneNumberIdDestino().isBlank()) {
            dados.put("phoneNumberIdDestino", request.getPhoneNumberIdDestino());
        }
    }

    private TipoAtendimento identificarIntencao(String texto) {
        String normalizado = normalizarTexto(texto);
        return switch (normalizado) {
            case "1" -> TipoAtendimento.ABRIR_EMPRESA;
            case "2" -> TipoAtendimento.REGULARIZAR_MEI;
            case "3" -> TipoAtendimento.EMITIR_DAS;
            case "4" -> TipoAtendimento.IMPOSTO_DE_RENDA;
            case "5" -> TipoAtendimento.FOLHA_DE_PAGAMENTO;
            case "6" -> TipoAtendimento.CERTIDOES_NEGATIVAS;
            case "7" -> TipoAtendimento.FALAR_COM_CONTADOR;
            case "8" -> TipoAtendimento.ENVIAR_DOCUMENTOS;
            default -> identificarPorTexto(normalizado);
        };
    }

    private TipoAtendimento identificarPorTexto(String texto) {
        if (texto.contains("abrir") || texto.contains("empresa")) {
            return TipoAtendimento.ABRIR_EMPRESA;
        }
        if (texto.contains("mei")) {
            return TipoAtendimento.REGULARIZAR_MEI;
        }
        if (texto.contains("das")) {
            return TipoAtendimento.EMITIR_DAS;
        }
        if (texto.contains("renda") || texto.contains("irpf")) {
            return TipoAtendimento.IMPOSTO_DE_RENDA;
        }
        if (texto.contains("folha") || texto.contains("pagamento")) {
            return TipoAtendimento.FOLHA_DE_PAGAMENTO;
        }
        if (texto.contains("certidao") || texto.contains("certidoes") || texto.contains("certidão") || texto.contains("certidões")) {
            return TipoAtendimento.CERTIDOES_NEGATIVAS;
        }
        if (texto.contains("contador") || texto.contains("humano")) {
            return TipoAtendimento.FALAR_COM_CONTADOR;
        }
        if (texto.contains("documento")) {
            return TipoAtendimento.ENVIAR_DOCUMENTOS;
        }
        return null;
    }

    private boolean deveEnviarMenu(String texto) {
        String normalizado = normalizarTexto(texto);
        return List.of("oi", "ola", "menu", "inicio", "opcoes", "bom dia", "boa tarde", "boa noite")
                .contains(normalizado);
    }

    private boolean isRespostaTriagemInicial(String texto) {
        String normalizado = normalizarTexto(texto);
        return List.of("1", "2", "3").contains(normalizado);
    }

    private String normalizarTexto(String texto) {
        String semAcentos = Normalizer.normalize(texto == null ? "" : texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return semAcentos
                .toLowerCase()
                .replaceAll("[^a-z0-9 ]", " ")
                .trim()
                .replaceAll("\\s+", " ");
    }

    private String mensagemInicialTriagem() {
        return """
                Olá! Você está falando com a %s.
                Antes de continuarmos, você já é cliente da ACSA Contabilidade?

                1 - Sim, já sou cliente
                2 - Não, ainda não sou cliente
                3 - Não tenho certeza
                """.formatted(NOME_CONTABILIDADE);
    }

    private String menuNovosClientes() {
        return """
                Como podemos ajudar?

                1 - Abrir empresa
                2 - Regularizar MEI
                3 - Emitir DAS
                4 - Imposto de Renda
                5 - Folha de pagamento
                6 - Certidões negativas
                7 - Falar com contador
                8 - Enviar documentos
                """;
    }

    private String menuDepartamentosCliente() {
        return """
                Qual departamento você deseja acionar?

                1 - Departamento Pessoal
                2 - Fiscal
                3 - Jurídico
                4 - Falar com contador
                """;
    }

    private String menuDepartamentoPessoal() {
        return """
                Sobre qual assunto do Departamento Pessoal você precisa de atendimento?

                1 - Folha de pagamento
                2 - Pró-labore
                3 - Recálculo de guia
                4 - INSS
                5 - FGTS
                6 - Falar com responsável do Departamento Pessoal
                7 - Voltar
                """;
    }

    private String menuFolhaPagamento() {
        return """
                Qual assunto de folha de pagamento você precisa tratar?

                1 - Férias
                2 - Rescisão
                3 - Admissão
                4 - Dúvida sobre folha mensal
                5 - Envio de documentos da folha
                6 - Outro assunto de folha de pagamento
                """;
    }

    private String mensagemInicial() {
        return """
                Olá! Você está falando com a %s.
                Como podemos ajudar?

                1 - Abrir empresa
                2 - Regularizar MEI
                3 - Emitir DAS
                4 - Imposto de Renda
                5 - Folha de pagamento
                6 - Certidões negativas
                7 - Falar com contador
                8 - Enviar documentos
                """.formatted(NOME_CONTABILIDADE);
    }

    private List<String> perguntas(TipoAtendimento tipo) {
        return switch (tipo) {
            case TRIAGEM_INICIAL, CLIENTE_DEPARTAMENTO_PESSOAL, CLIENTE_FISCAL, CLIENTE_PARALEGAL, CLIENTE_VERIFICACAO -> List.of();
            case ABRIR_EMPRESA -> List.of(
                    "Qual é o seu nome?",
                    "Qual é o seu telefone?",
                    "Qual é o seu e-mail?",
                    "Em qual cidade e estado a empresa será aberta?",
                    "Qual será a atividade principal da empresa?",
                    """
                    Qual tipo de empresa você pretende abrir?

                    1 - MEI
                    2 - ME
                    3 - LTDA
                    4 - Ainda não sei, quero orientação
                    """,
                    """
                    A empresa terá sócios?

                    1 - Sim
                    2 - Não
                    3 - Ainda não sei
                    """,
                    """
                    A empresa terá funcionários no início?

                    1 - Sim
                    2 - Não
                    3 - Ainda não sei
                    """,
                    """
                    Você já possui CNPJ?

                    1 - Sim
                    2 - Não
                    """
            );
            case REGULARIZAR_MEI -> List.of(
                    "Informe o CNPJ do MEI.",
                    """
                    Qual é a situação atual do MEI?

                    1 - Está ativo, mas tenho DAS em atraso
                    2 - Está ativo, mas preciso regularizar dados
                    3 - Está baixado/cancelado
                    4 - Está inapto ou com pendências
                    5 - Fui desenquadrado do MEI
                    6 - Não sei a situação atual
                    """,
                    """
                    Há DAS em atraso?

                    1 - Sim
                    2 - Não
                    3 - Não sei
                    """,
                    "Qual telefone para contato?"
            );
            case EMITIR_DAS -> List.of(
                    "Informe o CNPJ.",
                    "Qual é o mês de referência?",
                    "Qual é o e-mail que devemos usar para envio?"
            );
            case IMPOSTO_DE_RENDA -> List.of(
                    "Qual é o seu nome?",
                    "Informe o CPF.",
                    "Qual é o ano de referência?",
                    "Você possui informes de rendimento?"
            );
            case FOLHA_DE_PAGAMENTO -> List.of(
                    "Informe o CNPJ.",
                    "Quantos funcionários a empresa possui?",
                    "Qual é o mês de competência?"
            );
            case CERTIDOES_NEGATIVAS -> List.of(
                    "Informe o CNPJ.",
                    """
                    Qual tipo de certidão você precisa?

                    1 - Receita Federal / PGFN
                    2 - FGTS
                    3 - Trabalhista
                    4 - Estadual
                    5 - Municipal
                    6 - Falência e recuperação judicial
                    7 - Todas as certidões
                    8 - Não sei, quero ajuda do contador
                    """,
                    """
                    Qual é a finalidade da certidão?

                    Exemplos:
                    1 - Participar de licitação
                    2 - Abrir conta bancária
                    3 - Financiamento
                    4 - Cadastro em fornecedor
                    5 - Regularização da empresa
                    6 - Outra finalidade
                    """
            );
            case FALAR_COM_CONTADOR, ENVIAR_DOCUMENTOS -> List.of();
        };
    }

    private List<String> campos(TipoAtendimento tipo) {
        return switch (tipo) {
            case TRIAGEM_INICIAL, CLIENTE_DEPARTAMENTO_PESSOAL, CLIENTE_FISCAL, CLIENTE_PARALEGAL, CLIENTE_VERIFICACAO -> List.of();
            case ABRIR_EMPRESA -> List.of(
                    "nome",
                    "telefone",
                    "email",
                    "cidadeEstado",
                    "atividadePrincipal",
                    "tipoEmpresaPretendida",
                    "teraSocios",
                    "teraFuncionarios",
                    "possuiCnpj"
            );
            case REGULARIZAR_MEI -> List.of("cnpj", "situacaoAtual", "dasAtrasado", "telefone");
            case EMITIR_DAS -> List.of("cnpj", "mesReferencia", "emailEnvio");
            case IMPOSTO_DE_RENDA -> List.of("nome", "cpf", "anoReferencia", "possuiInformesRendimento");
            case FOLHA_DE_PAGAMENTO -> List.of("cnpj", "quantidadeFuncionarios", "mesCompetencia");
            case CERTIDOES_NEGATIVAS -> List.of("cnpj", "tipoCertidao", "finalidade");
            case FALAR_COM_CONTADOR, ENVIAR_DOCUMENTOS -> List.of();
        };
    }

    private Map<String, String> lerDados(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            log.warn("Dados coletados inválidos, reiniciando mapa");
            return new LinkedHashMap<>();
        }
    }

    private String escreverDados(Map<String, String> dados) {
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Não foi possível serializar dados do atendimento", ex);
        }
    }

    private void salvarMensagem(Cliente cliente, Atendimento atendimento, DirecaoMensagem direcao, String conteudo) {
        MensagemChat mensagem = new MensagemChat();
        mensagem.setCliente(cliente);
        mensagem.setAtendimento(atendimento);
        mensagem.setDirecao(direcao);
        mensagem.setConteudo(conteudo);
        mensagem.setCriadoEm(LocalDateTime.now());
        mensagemChatRepository.save(mensagem);
    }

    private MensagemChatDTO toDto(MensagemChat mensagem) {
        MensagemChatDTO dto = new MensagemChatDTO();
        dto.setId(mensagem.getId());
        dto.setAtendimentoId(mensagem.getAtendimento() == null ? null : mensagem.getAtendimento().getId());
        dto.setDirecao(mensagem.getDirecao());
        dto.setConteudo(mensagem.getConteudo());
        dto.setCriadoEm(mensagem.getCriadoEm());
        return dto;
    }

    private record CampoPergunta(String campo, String pergunta) {
    }
}
