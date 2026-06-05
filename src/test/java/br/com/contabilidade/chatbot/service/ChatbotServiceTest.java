package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.dto.ChatRequestDTO;
import br.com.contabilidade.chatbot.dto.ChatResponseDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.entity.TipoPessoa;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import br.com.contabilidade.chatbot.repository.MensagemChatRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ChatbotServiceTest {

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private MensagemChatRepository mensagemChatRepository;

    @Mock
    private ClienteService clienteService;

    private ChatbotService chatbotService;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                atendimentoRepository,
                mensagemChatRepository,
                clienteService,
                new ObjectMapper()
        );

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNome("Joao");
        cliente.setTelefone("11999990000");
        cliente.setEmail("joao@email.com");
        cliente.setCpfCnpj("12345678901");
        cliente.setTipoPessoa(TipoPessoa.FISICA);
        cliente.setStatus("ATIVO");
    }

    @Test
    void deveIniciarFluxoDeAberturaDeEmpresa() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.empty());
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> {
            Atendimento atendimento = invocation.getArgument(0);
            atendimento.setId(100L);
            return atendimento;
        });

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("1");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getAtendimentoId()).isEqualTo(100L);
        assertThat(response.getTipoAtendimento()).isEqualTo(TipoAtendimento.ABRIR_EMPRESA);
        assertThat(response.getResposta()).contains("nome");
    }

    @Test
    void deveFinalizarColetaERegistrarAtendimentoNovo() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(200L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.EMITIR_DAS);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(2);
        atendimento.setDadosColetados("{\"cnpj\":\"11222333000144\",\"mesReferencia\":\"05/2026\"}");
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("financeiro@email.com");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getStatus()).isEqualTo(StatusAtendimento.NOVO);
        assertThat(response.getResposta()).contains("Registrei sua solicitacao");
    }
}
