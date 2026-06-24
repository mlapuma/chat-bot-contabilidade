package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppTestRequestDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppTestResponseDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.TipoPessoa;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WhatsAppSimuladoService {

    private static final String CANAL = "WHATSAPP_SIMULADO";

    private final ClienteRepository clienteRepository;
    private final ChatbotService chatbotService;

    public WhatsAppSimuladoService(ClienteRepository clienteRepository, ChatbotService chatbotService) {
        this.clienteRepository = clienteRepository;
        this.chatbotService = chatbotService;
    }

    @Transactional
    public WhatsAppTestResponseDTO enviar(WhatsAppTestRequestDTO request) {
        Cliente cliente = clienteRepository.findFirstByTelefone(request.getTelefone())
                .orElseGet(() -> criarClienteTeste(request));

        ChatRequestDTO chatRequest = new ChatRequestDTO();
        chatRequest.setClienteId(cliente.getId());
        chatRequest.setMensagem(request.getMensagem());
        ChatResponseDTO chatResponse = chatbotService.enviar(chatRequest);

        return new WhatsAppTestResponseDTO(
                cliente.getId(),
                CANAL,
                cliente.getTelefone(),
                chatResponse.getResposta(),
                chatResponse.getAtendimentoId(),
                chatResponse.getTipoAtendimento(),
                chatResponse.getStatus()
        );
    }

    private Cliente criarClienteTeste(WhatsAppTestRequestDTO request) {
        Cliente cliente = new Cliente();
        cliente.setNome(request.getNome());
        cliente.setTelefone(request.getTelefone());
        cliente.setEmail("whatsapp.teste+%s@local.test".formatted(somenteDigitos(request.getTelefone())));
        cliente.setCpfCnpj("00000000000");
        cliente.setTipoPessoa(TipoPessoa.FISICA);
        cliente.setNomeEmpresa("Teste WhatsApp");
        cliente.setRegimeTributario("NAO_INFORMADO");
        cliente.setSegmento("Atendimento simulado");
        cliente.setStatus(CANAL);
        cliente.setCriadoEm(LocalDateTime.now());
        return clienteRepository.save(cliente);
    }

    private String somenteDigitos(String telefone) {
        String digitos = telefone.replaceAll("\\D", "");
        return digitos.isBlank() ? "cliente" : digitos;
    }
}
