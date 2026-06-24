package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppTestRequestDTO;
import br.com.contabilidade.chatbot.dto.WhatsAppTestResponseDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WhatsAppSimuladoServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private ChatbotService chatbotService;

    @InjectMocks
    private WhatsAppSimuladoService whatsAppSimuladoService;

    @Test
    void deveCriarClienteTesteEEnviarMensagemAoChatbot() {
        when(clienteRepository.findFirstByTelefone("+55 11 99999-0000")).thenReturn(Optional.empty());
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(50L);
            return cliente;
        });
        when(chatbotService.enviar(any(ChatRequestDTO.class))).thenReturn(
                new ChatResponseDTO("Qual é o seu nome?", 80L, TipoAtendimento.ABRIR_EMPRESA, StatusAtendimento.AGUARDANDO_CLIENTE)
        );

        WhatsAppTestRequestDTO request = new WhatsAppTestRequestDTO();
        request.setNome("Cliente WhatsApp Teste");
        request.setTelefone("+55 11 99999-0000");
        request.setMensagem("1");

        WhatsAppTestResponseDTO response = whatsAppSimuladoService.enviar(request);

        ArgumentCaptor<ChatRequestDTO> captor = ArgumentCaptor.forClass(ChatRequestDTO.class);
        verify(chatbotService).enviar(captor.capture());
        assertThat(captor.getValue().getClienteId()).isEqualTo(50L);
        assertThat(captor.getValue().getMensagem()).isEqualTo("1");
        assertThat(response.getCanal()).isEqualTo("WHATSAPP_SIMULADO");
        assertThat(response.getAtendimentoId()).isEqualTo(80L);
    }
}
