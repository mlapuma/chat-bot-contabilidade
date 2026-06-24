package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.config.WhatsAppProperties;
import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppWebhookResultDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

@ExtendWith(MockitoExtension.class)
class WhatsAppServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ChatbotService chatbotService;

    private WhatsAppService whatsAppService;

    @BeforeEach
    void setUp() {
        WhatsAppProperties properties = new WhatsAppProperties();
        properties.setVerifyToken("token-local");
        properties.setEnabled(false);
        whatsAppService = new WhatsAppService(properties, clienteRepository, chatbotService, RestClient.builder());
    }

    @Test
    void deveValidarWebhookComTokenCorreto() {
        assertThat(whatsAppService.verificarWebhook("subscribe", "token-local")).isTrue();
        assertThat(whatsAppService.verificarWebhook("subscribe", "errado")).isFalse();
    }

    @Test
    void deveProcessarMensagemDeTextoRecebidaDaMeta() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setId(7L);
        cliente.setTelefone("5511999990000");

        when(clienteRepository.findFirstByTelefone("5511999990000")).thenReturn(Optional.of(cliente));
        when(chatbotService.enviar(any(ChatRequestDTO.class))).thenReturn(
                new ChatResponseDTO("Informe o CNPJ.", 22L, TipoAtendimento.EMITIR_DAS, StatusAtendimento.AGUARDANDO_CLIENTE)
        );

        JsonNode payload = new ObjectMapper().readTree("""
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "5511999990000",
                          "type": "text",
                          "text": {"body": "3"}
                        }]
                      }
                    }]
                  }]
                }
                """);

        List<WhatsAppWebhookResultDTO> resultados = whatsAppService.processarWebhook(payload);

        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chatbotService).enviar(captor.capture());
        assertThat(captor.getValue().getClienteId()).isEqualTo(7L);
        assertThat(captor.getValue().getMensagem()).isEqualTo("3");
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getAtendimentoId()).isEqualTo(22L);
        assertThat(resultados.get(0).isRespostaEnviada()).isFalse();
    }

    @Test
    void deveEnviarCatalogoNoPrimeiroContatoWhatsApp() throws Exception {
        when(clienteRepository.findFirstByTelefone("5511888880000")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(33L);
            return cliente;
        });
        when(chatbotService.enviarMenuInicial(eq(33L), eq("1"))).thenReturn(
                new ChatResponseDTO("Olá! Você está falando com a ACSA Contabilidade.\n\n1 - Abrir empresa", null, null, null)
        );

        JsonNode payload = new ObjectMapper().readTree("""
                {
                  "object": "whatsapp_business_account",
                  "entry": [{
                    "changes": [{
                      "value": {
                        "messages": [{
                          "from": "5511888880000",
                          "type": "text",
                          "text": {"body": "1"}
                        }]
                      }
                    }]
                  }]
                }
                """);

        List<WhatsAppWebhookResultDTO> resultados = whatsAppService.processarWebhook(payload);

        verify(chatbotService).enviarMenuInicial(33L, "1");
        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).getAtendimentoId()).isNull();
        assertThat(resultados.get(0).getRespostaBot()).contains("Abrir empresa");
    }
}
