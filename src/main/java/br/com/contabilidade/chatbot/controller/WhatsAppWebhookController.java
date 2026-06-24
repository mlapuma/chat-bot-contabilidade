package br.com.contabilidade.chatbot.controller;

import br.com.contabilidade.chatbot.dto.WhatsAppWebhookResultDTO;
import br.com.contabilidade.chatbot.service.WhatsAppService;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/whatsapp/webhook")
public class WhatsAppWebhookController {

    private final WhatsAppService whatsAppService;

    public WhatsAppWebhookController(WhatsAppService whatsAppService) {
        this.whatsAppService = whatsAppService;
    }

    @GetMapping
    public ResponseEntity<String> verificarWebhook(@RequestParam(name = "hub.mode", required = false) String mode,
                                                   @RequestParam(name = "hub.verify_token", required = false) String verifyToken,
                                                   @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (whatsAppService.verificarWebhook(mode, verifyToken) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Token de verificação inválido");
    }

    @PostMapping
    public ResponseEntity<List<WhatsAppWebhookResultDTO>> receberMensagem(@RequestBody JsonNode payload) {
        return ResponseEntity.ok(whatsAppService.processarWebhook(payload));
    }
}
