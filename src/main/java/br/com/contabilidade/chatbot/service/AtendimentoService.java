package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.dto.RespostaAtendenteDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.DirecaoMensagem;
import br.com.contabilidade.chatbot.entity.MensagemChat;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.exception.RecursoNaoEncontradoException;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import br.com.contabilidade.chatbot.repository.MensagemChatRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;
    private final MensagemChatRepository mensagemChatRepository;
    private final WhatsAppService whatsAppService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AtendimentoService(AtendimentoRepository atendimentoRepository,
                              MensagemChatRepository mensagemChatRepository,
                              WhatsAppService whatsAppService) {
        this.atendimentoRepository = atendimentoRepository;
        this.mensagemChatRepository = mensagemChatRepository;
        this.whatsAppService = whatsAppService;
    }

    @Transactional(readOnly = true)
    public List<AtendimentoDTO> listar() {
        return listar(null);
    }

    @Transactional(readOnly = true)
    public List<AtendimentoDTO> listar(String departamento) {
        return atendimentoRepository.findAll().stream()
                .map(this::toDto)
                .filter(dto -> departamento == null
                        || departamento.isBlank()
                        || "TODOS".equalsIgnoreCase(departamento)
                        || departamento.equalsIgnoreCase(dto.getDepartamento()))
                .toList();
    }

    @Transactional(readOnly = true)
    public AtendimentoDTO buscar(Long id) {
        return toDto(buscarEntidade(id));
    }

    @Transactional
    public AtendimentoDTO atualizarStatus(Long id, StatusAtendimento status) {
        Atendimento atendimento = buscarEntidade(id);
        atendimento.setStatus(status);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return toDto(atendimentoRepository.save(atendimento));
    }

    @Transactional
    public AtendimentoDTO assumir(Long id, String atendente) {
        Atendimento atendimento = buscarEntidade(id);
        atendimento.setAtendenteResponsavel(atendente.trim());
        atendimento.setAssumidoEm(LocalDateTime.now());
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return toDto(atendimentoRepository.save(atendimento));
    }

    @Transactional
    public AtendimentoDTO liberar(Long id) {
        Atendimento atendimento = buscarEntidade(id);
        atendimento.setAtendenteResponsavel(null);
        atendimento.setAssumidoEm(null);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return toDto(atendimentoRepository.save(atendimento));
    }

    @Transactional
    public RespostaAtendenteDTO responder(Long id, String atendente, String mensagem) {
        Atendimento atendimento = buscarEntidade(id);
        if (atendimento.getCliente() == null || atendimento.getCliente().getTelefone() == null
                || atendimento.getCliente().getTelefone().isBlank()) {
            throw new IllegalArgumentException("Atendimento não possui cliente com telefone para resposta");
        }

        String nomeAtendente = atendente.trim();
        atendimento.setAtendenteResponsavel(nomeAtendente);
        if (atendimento.getAssumidoEm() == null) {
            atendimento.setAssumidoEm(LocalDateTime.now());
        }
        atendimento.setStatus(StatusAtendimento.EM_ANDAMENTO);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        Atendimento salvo = atendimentoRepository.save(atendimento);

        MensagemChat mensagemChat = new MensagemChat();
        mensagemChat.setCliente(salvo.getCliente());
        mensagemChat.setAtendimento(salvo);
        mensagemChat.setDirecao(DirecaoMensagem.HUMANO);
        mensagemChat.setConteudo("%s: %s".formatted(nomeAtendente, mensagem.trim()));
        mensagemChat.setCriadoEm(LocalDateTime.now());
        mensagemChatRepository.save(mensagemChat);

        boolean enviado = whatsAppService.enviarMensagemSeConfigurado(salvo.getCliente().getTelefone(), mensagem.trim());
        String retorno = enviado
                ? "Mensagem enviada ao cliente pelo WhatsApp."
                : "Mensagem registrada. O envio pelo WhatsApp não está configurado ou falhou.";
        return new RespostaAtendenteDTO(toDto(salvo), enviado, retorno);
    }

    public Atendimento buscarEntidade(Long id) {
        return atendimentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento não encontrado"));
    }

    AtendimentoDTO toDto(Atendimento atendimento) {
        AtendimentoDTO dto = new AtendimentoDTO();
        dto.setId(atendimento.getId());
        if (atendimento.getCliente() != null) {
            dto.setClienteId(atendimento.getCliente().getId());
            dto.setNomeCliente(atendimento.getCliente().getNome());
            dto.setTelefoneCliente(atendimento.getCliente().getTelefone());
        }
        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
        dto.setTipoAtendimento(atendimento.getTipoAtendimento());
        dto.setDepartamento(departamentoDoAtendimento(atendimento));
        dto.setNumeroWhatsAppDestino(dados.get("numeroWhatsAppDestino"));
        dto.setPhoneNumberIdDestino(dados.get("phoneNumberIdDestino"));
        dto.setStatus(atendimento.getStatus());
        dto.setDadosColetados(atendimento.getDadosColetados());
        dto.setAtendenteResponsavel(atendimento.getAtendenteResponsavel());
        dto.setAssumidoEm(atendimento.getAssumidoEm());
        dto.setCriadoEm(atendimento.getCriadoEm());
        dto.setAtualizadoEm(atendimento.getAtualizadoEm());
        return dto;
    }

    private String departamentoDoAtendimento(Atendimento atendimento) {
        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
        String departamento = dados.get("departamento");
        if (departamento != null && !departamento.isBlank()) {
            return departamento;
        }

        TipoAtendimento tipo = atendimento.getTipoAtendimento();
        if (tipo == TipoAtendimento.CLIENTE_DEPARTAMENTO_PESSOAL || tipo == TipoAtendimento.FOLHA_DE_PAGAMENTO) {
            return "Departamento Pessoal";
        }
        if (tipo == TipoAtendimento.CLIENTE_FISCAL || tipo == TipoAtendimento.EMITIR_DAS) {
            return "Fiscal";
        }
        if (tipo == TipoAtendimento.CLIENTE_PARALEGAL || tipo == TipoAtendimento.ABRIR_EMPRESA || tipo == TipoAtendimento.CERTIDOES_NEGATIVAS) {
            return "Jurídico";
        }
        return "Contabilidade";
    }

    private Map<String, String> lerDados(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }
}
