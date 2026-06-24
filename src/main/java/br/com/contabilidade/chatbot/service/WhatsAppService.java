package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.config.WhatsAppProperties;
import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppWebhookResultDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.TipoPessoa;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Service
public class WhatsAppService implements CanalMensagemService {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppService.class);
    private static final String CANAL = "WHATSAPP_CLOUD_API";

    private final WhatsAppProperties properties;
    private final ClienteRepository clienteRepository;
    private final ChatbotService chatbotService;
    private final RestClient restClient;

    public WhatsAppService(WhatsAppProperties properties,
                           ClienteRepository clienteRepository,
                           ChatbotService chatbotService,
                           RestClient.Builder restClientBuilder) {
        this.properties = properties;
        this.clienteRepository = clienteRepository;
        this.chatbotService = chatbotService;
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
    }

    public boolean verificarWebhook(String mode, String verifyToken) {
        return "subscribe".equals(mode)
                && properties.getVerifyToken() != null
                && properties.getVerifyToken().equals(verifyToken);
    }

    @Transactional
    public List<WhatsAppWebhookResultDTO> processarWebhook(JsonNode payload) {
        List<WhatsAppWebhookResultDTO> resultados = new ArrayList<>();
        JsonNode entries = payload.path("entry");
        if (!entries.isArray()) {
            return resultados;
        }

        for (JsonNode entry : entries) {
            JsonNode changes = entry.path("changes");
            if (!changes.isArray()) {
                continue;
            }
            for (JsonNode change : changes) {
                JsonNode messages = change.path("value").path("messages");
                if (!messages.isArray()) {
                    continue;
                }
                JsonNode metadata = change.path("value").path("metadata");
                for (JsonNode message : messages) {
                    resultadoDaMensagem(message, metadata).ifPresent(resultados::add);
                }
            }
        }
        return resultados;
    }

    private java.util.Optional<WhatsAppWebhookResultDTO> resultadoDaMensagem(JsonNode message, JsonNode metadata) {
        String telefoneCliente = message.path("from").asText();
        String tipo = message.path("type").asText();
        String texto = extrairTexto(message, tipo);
        String numeroDestino = metadata.path("display_phone_number").asText("");
        String phoneNumberIdDestino = metadata.path("phone_number_id").asText("");

        if (telefoneCliente.isBlank() || texto.isBlank()) {
            log.info("Webhook WhatsApp ignorado: tipo={} telefonePresente={}", tipo, !telefoneCliente.isBlank());
            return java.util.Optional.empty();
        }

        Optional<Cliente> clienteExistente = clienteRepository.findFirstByTelefone(telefoneCliente);
        Cliente cliente = clienteExistente.orElseGet(() -> criarClienteWhatsApp(telefoneCliente));

        ChatResponseDTO response;
        if (clienteExistente.isEmpty()) {
            response = chatbotService.enviarMenuInicial(cliente.getId(), texto);
        } else {
            ChatRequestDTO request = new ChatRequestDTO();
            request.setClienteId(cliente.getId());
            request.setMensagem(texto);
            request.setNumeroWhatsAppDestino(numeroDestino);
            request.setPhoneNumberIdDestino(phoneNumberIdDestino);
            response = chatbotService.enviar(request);
        }

        boolean enviado = enviarMensagemSeConfigurado(telefoneCliente, response.getResposta());
        log.info("Webhook WhatsApp processado: origem={} tipo={} clienteId={} atendimentoId={} respostaEnviada={}",
                telefoneCliente, tipo, cliente.getId(), response.getAtendimentoId(), enviado);
        return java.util.Optional.of(new WhatsAppWebhookResultDTO(
                telefoneCliente,
                texto,
                response.getResposta(),
                cliente.getId(),
                response.getAtendimentoId(),
                enviado
        ));
    }

    private String extrairTexto(JsonNode message, String tipo) {
        if ("text".equals(tipo)) {
            return message.path("text").path("body").asText("");
        }
        if ("button".equals(tipo)) {
            return message.path("button").path("text").asText("");
        }
        if ("interactive".equals(tipo)) {
            JsonNode interactive = message.path("interactive");
            String buttonReply = interactive.path("button_reply").path("title").asText("");
            if (!buttonReply.isBlank()) {
                return buttonReply;
            }
            return interactive.path("list_reply").path("title").asText("");
        }
        return "";
    }

    private Cliente criarClienteWhatsApp(String telefoneCliente) {
        Cliente cliente = new Cliente();
        cliente.setNome("Cliente WhatsApp " + telefoneCliente);
        cliente.setTelefone(telefoneCliente);
        cliente.setEmail("whatsapp.cloud+%s@local.test".formatted(telefoneCliente.replaceAll("\\D", "")));
        cliente.setCpfCnpj("00000000000");
        cliente.setTipoPessoa(TipoPessoa.FISICA);
        cliente.setNomeEmpresa("WhatsApp Cloud API");
        cliente.setRegimeTributario("NAO_INFORMADO");
        cliente.setSegmento("Atendimento WhatsApp");
        cliente.setStatus(CANAL);
        cliente.setCriadoEm(LocalDateTime.now());
        return clienteRepository.save(cliente);
    }

    public boolean enviarMensagemSeConfigurado(String telefoneCliente, String resposta) {
        if (!properties.envioConfigurado()) {
            log.info("Resposta WhatsApp não enviada porque a Cloud API não está configurada");
            return false;
        }
        try {
            enviarMensagem(telefoneCliente, resposta);
            return true;
        } catch (RestClientResponseException ex) {
            log.warn("Não foi possível enviar resposta WhatsApp para destino={}. Graph API status={} body={}",
                    telefoneCliente, ex.getStatusCode().value(), limitar(ex.getResponseBodyAsString()));
            return false;
        } catch (RuntimeException ex) {
            log.warn("Não foi possível enviar resposta WhatsApp para destino={}. Verifique token, phone number id e destinatário autorizado.", telefoneCliente);
            return false;
        }
    }

    @Override
    public void enviarMensagem(String destino, String mensagem) {
        if (!properties.envioConfigurado()) {
            log.info("Envio WhatsApp ignorado: configure WHATSAPP_CLOUD_ENABLED, PHONE_NUMBER_ID e ACCESS_TOKEN");
            return;
        }

        Map<String, Object> body = Map.of(
                "messaging_product", "whatsapp",
                "recipient_type", "individual",
                "to", destino,
                "type", "text",
                "text", Map.of(
                        "preview_url", false,
                        "body", mensagem
                )
        );

        restClient.post()
                .uri("/{version}/{phoneNumberId}/messages", properties.getApiVersion(), properties.getPhoneNumberId())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getAccessToken())
                .body(body)
                .retrieve()
                .toBodilessEntity();
    }

    private String limitar(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return texto.length() > 500 ? texto.substring(0, 500) : texto;
    }
}
