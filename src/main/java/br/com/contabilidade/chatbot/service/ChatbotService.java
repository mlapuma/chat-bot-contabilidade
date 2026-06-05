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

    private final AtendimentoRepository atendimentoRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final ClienteService clienteService;
    private final ObjectMapper objectMapper;

    public ChatbotService(AtendimentoRepository atendimentoRepository,
                          MensagemChatRepository mensagemChatRepository,
                          ClienteService clienteService,
                          ObjectMapper objectMapper) {
        this.atendimentoRepository = atendimentoRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.clienteService = clienteService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ChatResponseDTO enviar(ChatRequestDTO request) {
        Cliente cliente = request.getClienteId() == null ? null : clienteService.buscarEntidade(request.getClienteId());
        String texto = request.getMensagem().trim();

        Optional<Atendimento> atendimentoAberto = cliente == null
                ? Optional.empty()
                : atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(cliente.getId(), STATUS_ABERTOS);

        if (atendimentoAberto.isPresent()) {
            return continuarAtendimento(atendimentoAberto.get(), cliente, texto);
        }

        TipoAtendimento tipo = identificarIntencao(texto);
        if (tipo == null) {
            String resposta = mensagemInicial();
            salvarMensagem(cliente, null, DirecaoMensagem.CLIENTE, texto);
            salvarMensagem(cliente, null, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, null, null, null);
        }

        Atendimento atendimento = criarAtendimento(cliente, tipo);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        if (tipo == TipoAtendimento.FALAR_COM_CONTADOR) {
            atendimento.setStatus(StatusAtendimento.AGUARDANDO_HUMANO);
            atendimento.setDadosColetados("{}");
            atendimentoRepository.save(atendimento);
            String resposta = "Certo. Seu atendimento foi encaminhado para um contador. Em breve nossa equipe entrara em contato.";
            salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, atendimento.getId(), tipo, atendimento.getStatus());
        }

        if (tipo == TipoAtendimento.ENVIAR_DOCUMENTOS) {
            atendimento.setStatus(StatusAtendimento.DOCUMENTOS_PENDENTES);
            atendimento.setDadosColetados("{}");
            atendimentoRepository.save(atendimento);
            String resposta = "Voce pode enviar os documentos pelo canal combinado com o escritorio. Caso esteja no WhatsApp, anexe os arquivos nesta conversa. Seu atendimento ficou como documentos pendentes.";
            salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
            return new ChatResponseDTO(resposta, atendimento.getId(), tipo, atendimento.getStatus());
        }

        String primeiraPergunta = perguntas(tipo).get(0);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, primeiraPergunta);
        return new ChatResponseDTO(primeiraPergunta, atendimento.getId(), tipo, atendimento.getStatus());
    }

    @Transactional(readOnly = true)
    public List<MensagemChatDTO> historico(Long clienteId) {
        return mensagemChatRepository.findByClienteIdOrderByCriadoEmAsc(clienteId).stream()
                .map(this::toDto)
                .toList();
    }

    private ChatResponseDTO continuarAtendimento(Atendimento atendimento, Cliente cliente, String texto) {
        salvarMensagem(cliente, atendimento, DirecaoMensagem.CLIENTE, texto);

        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
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
            resposta = "Obrigado. Registrei sua solicitacao e um contador entrara em contato para dar continuidade ao atendimento.";
            log.info("Atendimento {} registrado com tipo={}", atendimento.getId(), atendimento.getTipoAtendimento());
        } else {
            atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
            resposta = perguntas(atendimento.getTipoAtendimento()).get(etapa);
        }

        atendimentoRepository.save(atendimento);
        salvarMensagem(cliente, atendimento, DirecaoMensagem.BOT, resposta);
        return new ChatResponseDTO(resposta, atendimento.getId(), atendimento.getTipoAtendimento(), atendimento.getStatus());
    }

    private Atendimento criarAtendimento(Cliente cliente, TipoAtendimento tipo) {
        Atendimento atendimento = new Atendimento();
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(tipo);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(0);
        atendimento.setDadosColetados("{}");
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return atendimentoRepository.save(atendimento);
    }

    private TipoAtendimento identificarIntencao(String texto) {
        String normalizado = texto.toLowerCase();
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
        if (texto.contains("certidao") || texto.contains("certidoes")) {
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

    private String mensagemInicial() {
        return """
                Ola! Seja bem-vindo ao atendimento do escritorio contabil. Como posso ajudar?

                1 - Abrir empresa
                2 - Regularizar MEI
                3 - Emitir DAS
                4 - Imposto de Renda
                5 - Folha de pagamento
                6 - Certidoes negativas
                7 - Falar com contador
                8 - Enviar documentos
                """;
    }

    private List<String> perguntas(TipoAtendimento tipo) {
        return switch (tipo) {
            case ABRIR_EMPRESA -> List.of(
                    "Qual e o seu nome?",
                    "Qual e o seu telefone?",
                    "Qual e o seu e-mail?",
                    "Em qual cidade a empresa sera aberta?",
                    "Qual sera a atividade da empresa?",
                    "Voce ja possui CNPJ?"
            );
            case REGULARIZAR_MEI -> List.of(
                    "Informe o CNPJ do MEI.",
                    "Qual e a situacao atual do MEI?",
                    "Ha DAS atrasado?",
                    "Qual telefone para contato?"
            );
            case EMITIR_DAS -> List.of(
                    "Informe o CNPJ.",
                    "Qual e o mes de referencia?",
                    "Qual e-mail devemos usar para envio?"
            );
            case IMPOSTO_DE_RENDA -> List.of(
                    "Qual e o seu nome?",
                    "Informe o CPF.",
                    "Qual e o ano de referencia?",
                    "Voce possui informes de rendimento?"
            );
            case FOLHA_DE_PAGAMENTO -> List.of(
                    "Informe o CNPJ.",
                    "Quantos funcionarios a empresa possui?",
                    "Qual e o mes de competencia?"
            );
            case CERTIDOES_NEGATIVAS -> List.of(
                    "Informe o CNPJ.",
                    "Qual tipo de certidao voce precisa?",
                    "Qual e a finalidade da certidao?"
            );
            case FALAR_COM_CONTADOR, ENVIAR_DOCUMENTOS -> List.of();
        };
    }

    private List<String> campos(TipoAtendimento tipo) {
        return switch (tipo) {
            case ABRIR_EMPRESA -> List.of("nome", "telefone", "email", "cidade", "atividadeEmpresa", "possuiCnpj");
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
            log.warn("Dados coletados invalidos, reiniciando mapa");
            return new LinkedHashMap<>();
        }
    }

    private String escreverDados(Map<String, String> dados) {
        try {
            return objectMapper.writeValueAsString(dados);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Nao foi possivel serializar dados do atendimento", ex);
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
}
