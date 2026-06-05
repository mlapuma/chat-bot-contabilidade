package br.com.contabilidade.chatbot.controller;

import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.MensagemChatDTO;
import br.com.contabilidade.chatbot.service.ChatbotService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatbotService chatbotService;

    public ChatController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @PostMapping("/enviar")
    public ChatResponseDTO enviar(@Valid @RequestBody ChatRequestDTO request) {
        return chatbotService.enviar(request);
    }

    @GetMapping("/historico/{clienteId}")
    public List<MensagemChatDTO> historico(@PathVariable Long clienteId) {
        return chatbotService.historico(clienteId);
    }
}
