package br.com.contabilidade.chatbot.controller;

import br.com.contabilidade.chatbot.dto.WhatsAppTestRequestDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppTestResponseDTO;
import br.com.contabilidade.chatbot.service.WhatsAppSimuladoService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/testes/whatsapp")
public class TesteWhatsAppController {

    private final WhatsAppSimuladoService whatsAppSimuladoService;

    public TesteWhatsAppController(WhatsAppSimuladoService whatsAppSimuladoService) {
        this.whatsAppSimuladoService = whatsAppSimuladoService;
    }

    @PostMapping("/enviar")
    public WhatsAppTestResponseDTO enviar(@Valid @RequestBody WhatsAppTestRequestDTO request) {
        return whatsAppSimuladoService.enviar(request);
    }
}
