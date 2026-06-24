package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
import java.util.List;
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

    @Mock
    private DasEmissionService dasEmissionService;

    private ChatbotService chatbotService;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        chatbotService = new ChatbotService(
                atendimentoRepository,
                mensagemChatRepository,
                clienteService,
                new ObjectMapper(),
                dasEmissionService
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
        request.setMensagem("abrir empresa");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getAtendimentoId()).isEqualTo(100L);
        assertThat(response.getTipoAtendimento()).isEqualTo(TipoAtendimento.ABRIR_EMPRESA);
        assertThat(response.getResposta()).contains("nome");
    }

    @Test
    void deveGuiarClienteNoTipoDeEmpresaParaAbertura() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(500L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.ABRIR_EMPRESA);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(4);
        atendimento.setDadosColetados("""
                {
                  "nome":"Maria Souza",
                  "telefone":"11999990000",
                  "email":"maria@email.com",
                  "cidadeEstado":"São Paulo/SP"
                }
                """);

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("Prestação de serviços administrativos");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getResposta()).contains("Qual tipo de empresa");
        assertThat(response.getResposta()).contains("MEI");
        assertThat(response.getResposta()).contains("ME");
        assertThat(response.getResposta()).contains("LTDA");
        assertThat(response.getResposta()).contains("Ainda não sei, quero orientação");
        assertThat(atendimento.getDadosColetados()).contains("atividadePrincipal");
    }

    @Test
    void deveEnviarMenuInicialSemAbrirAtendimento() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);

        ChatResponseDTO response = chatbotService.enviarMenuInicial(1L, "Oi");

        assertThat(response.getAtendimentoId()).isNull();
        assertThat(response.getTipoAtendimento()).isNull();
        assertThat(response.getResposta()).contains("ACSA Contabilidade");
        assertThat(response.getResposta()).contains("você já é cliente");
        assertThat(response.getResposta()).contains("1 - Sim, já sou cliente");
        assertThat(response.getResposta()).contains("2 - Não, ainda não sou cliente");
    }

    @Test
    void deveEnviarMenuQuandoClienteEnviarOiMesmoComAtendimentoAberto() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(200L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.EMITIR_DAS);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(1);
        atendimento.setDadosColetados("{\"cnpj\":\"11222333000144\"}");

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findByClienteIdAndStatusIn(any(), anyCollection()))
                .thenReturn(List.of(atendimento));
        when(atendimentoRepository.saveAll(anyCollection())).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("oi");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getAtendimentoId()).isNull();
        assertThat(response.getResposta()).contains("ACSA Contabilidade");
        assertThat(response.getResposta()).contains("você já é cliente");
        assertThat(response.getResposta()).contains("1 - Sim, já sou cliente");
        assertThat(atendimento.getStatus()).isEqualTo(StatusAtendimento.FINALIZADO);
        verify(atendimentoRepository).saveAll(anyCollection());
        verify(atendimentoRepository, never()).save(any(Atendimento.class));
    }

    @Test
    void deveAbrirMenuDeDepartamentosParaClienteExistente() {
        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.empty());
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> {
            Atendimento atendimento = invocation.getArgument(0);
            atendimento.setId(700L);
            return atendimento;
        });

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("1");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getAtendimentoId()).isEqualTo(700L);
        assertThat(response.getTipoAtendimento()).isEqualTo(TipoAtendimento.TRIAGEM_INICIAL);
        assertThat(response.getResposta()).contains("Qual departamento");
        assertThat(response.getResposta()).contains("Departamento Pessoal");
        assertThat(response.getResposta()).contains("Fiscal");
        assertThat(response.getResposta()).contains("Jur");
    }

    @Test
    void deveAbrirSubmenuDoDepartamentoPessoal() {
        Atendimento atendimento = atendimentoTriagem("{\"perfilCliente\":\"JA_CLIENTE\",\"estadoTriagem\":\"cliente_departamento\"}");

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("1");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getResposta()).contains("Departamento Pessoal");
        assertThat(response.getResposta()).contains("Folha de pagamento");
        assertThat(response.getResposta()).contains("Pr");
        assertThat(response.getResposta()).contains("INSS");
        assertThat(response.getResposta()).contains("FGTS");
        assertThat(atendimento.getTipoAtendimento()).isEqualTo(TipoAtendimento.TRIAGEM_INICIAL);
    }

    @Test
    void deveAbrirSubmenuDeFolhaDePagamento() {
        Atendimento atendimento = atendimentoTriagem("{\"perfilCliente\":\"JA_CLIENTE\",\"estadoTriagem\":\"dp_assunto\",\"departamento\":\"Departamento Pessoal\"}");

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("1");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getResposta()).contains("folha de pagamento");
        assertThat(response.getResposta()).contains("F");
        assertThat(response.getResposta()).contains("Rescis");
        assertThat(response.getResposta()).contains("Admiss");
        assertThat(atendimento.getTipoAtendimento()).isEqualTo(TipoAtendimento.TRIAGEM_INICIAL);
    }

    @Test
    void deveIniciarColetaDoAssuntoDeFolhaDePagamento() {
        Atendimento atendimento = atendimentoTriagem("""
                {
                  "perfilCliente":"JA_CLIENTE",
                  "estadoTriagem":"dp_folha_assunto",
                  "departamento":"Departamento Pessoal",
                  "assuntoDepartamentoPessoal":"Folha de pagamento"
                }
                """);

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("2");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getStatus()).isEqualTo(StatusAtendimento.AGUARDANDO_CLIENTE);
        assertThat(response.getTipoAtendimento()).isEqualTo(TipoAtendimento.FOLHA_DE_PAGAMENTO);
        assertThat(response.getResposta()).contains("CNPJ");
        assertThat(atendimento.getDadosColetados()).contains("assuntoFolhaPagamento");
        assertThat(atendimento.getDadosColetados()).contains("roteiroColeta");
    }

    @Test
    void deveColetarDadosAteRegistrarProLabore() {
        Atendimento atendimento = atendimentoTriagem("""
                {
                  "perfilCliente":"JA_CLIENTE",
                  "estadoTriagem":"dp_coleta",
                  "departamento":"Departamento Pessoal",
                  "assunto":"Pró-labore",
                  "roteiroColeta":"DP_PRO_LABORE",
                  "indiceColeta":"5",
                  "cnpj":"11222333000144",
                  "nomeSocio":"Maria Souza",
                  "cpfSocio":"12345678901",
                  "valorProLabore":"R$ 2.500,00",
                  "mesReferencia":"06/2026"
                }
                """);
        atendimento.setTipoAtendimento(TipoAtendimento.CLIENTE_DEPARTAMENTO_PESSOAL);

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("alteração");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getStatus()).isEqualTo(StatusAtendimento.NOVO);
        assertThat(response.getTipoAtendimento()).isEqualTo(TipoAtendimento.CLIENTE_DEPARTAMENTO_PESSOAL);
        assertThat(response.getResposta()).contains("informações necessárias");
        assertThat(atendimento.getDadosColetados()).contains("tipoSolicitacao");
    }

    @Test
    void deveGuiarClienteNasOpcoesDeCertidoesNegativas() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(300L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.CERTIDOES_NEGATIVAS);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(0);
        atendimento.setDadosColetados("{}");

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("12345678000190");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getResposta()).contains("Receita Federal / PGFN");
        assertThat(response.getResposta()).contains("FGTS");
        assertThat(response.getResposta()).contains("Todas as certidões");
        assertThat(response.getResposta()).contains("Não sei, quero ajuda do contador");
    }

    @Test
    void deveGuiarClienteNaSituacaoAtualDoMei() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(400L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.REGULARIZAR_MEI);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(0);
        atendimento.setDadosColetados("{}");

        when(clienteService.buscarEntidade(1L)).thenReturn(cliente);
        when(atendimentoRepository.findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(any(), anyCollection()))
                .thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("12345678000190");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getResposta()).contains("Está ativo, mas tenho DAS em atraso");
        assertThat(response.getResposta()).contains("Está baixado/cancelado");
        assertThat(response.getResposta()).contains("Fui desenquadrado do MEI");
        assertThat(response.getResposta()).contains("Não sei a situação atual");
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
        when(dasEmissionService.emitir(any(DasEmissionRequest.class)))
                .thenReturn(new DasEmissionResult(
                        "MANUAL_ASSISTIDO",
                        "AGUARDANDO_EMISSAO_OFICIAL",
                        "generated/das/das-atendimento-200.pdf",
                        "/api/atendimentos/200/das-pdf",
                        "O PDF interno da solicitação do DAS foi gerado para uso da equipe contábil. O contador fará a emissão oficial e entrará em contato com você."
                ));

        ChatRequestDTO request = new ChatRequestDTO();
        request.setClienteId(1L);
        request.setMensagem("financeiro@email.com");

        ChatResponseDTO response = chatbotService.enviar(request);

        assertThat(response.getStatus()).isEqualTo(StatusAtendimento.NOVO);
        assertThat(response.getResposta()).contains("Registrei sua solicitação");
        assertThat(response.getResposta()).contains("contador fará a emissão oficial");
        assertThat(response.getResposta()).doesNotContain("/api/");
        assertThat(atendimento.getDadosColetados()).contains("MANUAL_ASSISTIDO");
        assertThat(atendimento.getDadosColetados()).contains("AGUARDANDO_EMISSAO_OFICIAL");
        verify(dasEmissionService).emitir(any(DasEmissionRequest.class));
    }

    private Atendimento atendimentoTriagem(String dadosColetados) {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(700L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.TRIAGEM_INICIAL);
        atendimento.setStatus(StatusAtendimento.AGUARDANDO_CLIENTE);
        atendimento.setEtapaAtual(0);
        atendimento.setDadosColetados(dadosColetados);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return atendimento;
    }
}
