package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.dto.RespostaAtendenteDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.MensagemChat;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import br.com.contabilidade.chatbot.repository.MensagemChatRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AtendimentoServiceTest {

    @Mock
    private AtendimentoRepository atendimentoRepository;

    @Mock
    private MensagemChatRepository mensagemChatRepository;

    @Mock
    private WhatsAppService whatsAppService;

    @InjectMocks
    private AtendimentoService atendimentoService;

    @Test
    void deveAtualizarStatusDoAtendimento() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(10L);
        atendimento.setTipoAtendimento(TipoAtendimento.EMITIR_DAS);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setEtapaAtual(0);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());

        when(atendimentoRepository.findById(10L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtendimentoDTO atualizado = atendimentoService.atualizarStatus(10L, StatusAtendimento.FINALIZADO);

        assertThat(atualizado.getStatus()).isEqualTo(StatusAtendimento.FINALIZADO);
        assertThat(atualizado.getAtualizadoEm()).isNotNull();
    }

    @Test
    void deveAssumirAtendimentoParaAtendente() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(11L);
        atendimento.setTipoAtendimento(TipoAtendimento.FALAR_COM_CONTADOR);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setEtapaAtual(0);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());

        when(atendimentoRepository.findById(11L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AtendimentoDTO atualizado = atendimentoService.assumir(11L, "Ana");

        assertThat(atualizado.getStatus()).isEqualTo(StatusAtendimento.EM_ANDAMENTO);
        assertThat(atualizado.getAtendenteResponsavel()).isEqualTo("Ana");
        assertThat(atualizado.getAssumidoEm()).isNotNull();
    }

    @Test
    void deveListarApenasAtendimentosDoDepartamentoInformado() {
        Atendimento dp = atendimento(20L, TipoAtendimento.CLIENTE_DEPARTAMENTO_PESSOAL, "{\"departamento\":\"Departamento Pessoal\"}");
        Atendimento fiscal = atendimento(21L, TipoAtendimento.CLIENTE_FISCAL, "{\"departamento\":\"Fiscal\"}");
        Atendimento juridico = atendimento(22L, TipoAtendimento.CLIENTE_PARALEGAL, "{\"departamento\":\"Jurídico\"}");

        when(atendimentoRepository.findAll()).thenReturn(List.of(dp, fiscal, juridico));

        List<AtendimentoDTO> atendimentos = atendimentoService.listar("Fiscal");

        assertThat(atendimentos).hasSize(1);
        assertThat(atendimentos.get(0).getId()).isEqualTo(21L);
        assertThat(atendimentos.get(0).getDepartamento()).isEqualTo("Fiscal");
    }

    @Test
    void deveIdentificarDepartamentoPessoalPeloTipoQuandoNaoHouverDadosColetados() {
        Atendimento atendimento = atendimento(23L, TipoAtendimento.FOLHA_DE_PAGAMENTO, "{}");
        when(atendimentoRepository.findAll()).thenReturn(List.of(atendimento));

        List<AtendimentoDTO> atendimentos = atendimentoService.listar("Departamento Pessoal");

        assertThat(atendimentos).hasSize(1);
        assertThat(atendimentos.get(0).getDepartamento()).isEqualTo("Departamento Pessoal");
    }

    @Test
    void deveExibirOrigemWhatsAppDoAtendimento() {
        Cliente cliente = new Cliente();
        cliente.setId(33L);
        cliente.setNome("Cliente WhatsApp");
        cliente.setTelefone("5511999990000");

        Atendimento atendimento = atendimento(
                24L,
                TipoAtendimento.CLIENTE_FISCAL,
                "{\"departamento\":\"Fiscal\",\"numeroWhatsAppDestino\":\"+55 11 3000-1000\",\"phoneNumberIdDestino\":\"123456\"}"
        );
        atendimento.setCliente(cliente);

        when(atendimentoRepository.findAll()).thenReturn(List.of(atendimento));

        AtendimentoDTO dto = atendimentoService.listar("Fiscal").get(0);

        assertThat(dto.getTelefoneCliente()).isEqualTo("5511999990000");
        assertThat(dto.getNumeroWhatsAppDestino()).isEqualTo("+55 11 3000-1000");
        assertThat(dto.getPhoneNumberIdDestino()).isEqualTo("123456");
    }

    @Test
    void deveRegistrarRespostaHumanaEEnviarWhatsApp() {
        Cliente cliente = new Cliente();
        cliente.setId(3L);
        cliente.setNome("Cliente WhatsApp");
        cliente.setTelefone("5511999990000");

        Atendimento atendimento = new Atendimento();
        atendimento.setId(12L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.FALAR_COM_CONTADOR);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setEtapaAtual(0);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());

        when(atendimentoRepository.findById(12L)).thenReturn(Optional.of(atendimento));
        when(atendimentoRepository.save(any(Atendimento.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mensagemChatRepository.save(any(MensagemChat.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(whatsAppService.enviarMensagemSeConfigurado("5511999990000", "Olá, vou te ajudar.")).thenReturn(true);

        RespostaAtendenteDTO resposta = atendimentoService.responder(12L, "Bruno", "Olá, vou te ajudar.");

        assertThat(resposta.isEnviadoWhatsApp()).isTrue();
        assertThat(resposta.getAtendimento().getAtendenteResponsavel()).isEqualTo("Bruno");
        verify(mensagemChatRepository).save(any(MensagemChat.class));
        verify(whatsAppService).enviarMensagemSeConfigurado("5511999990000", "Olá, vou te ajudar.");
    }

    private Atendimento atendimento(Long id, TipoAtendimento tipo, String dadosColetados) {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(id);
        atendimento.setTipoAtendimento(tipo);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setEtapaAtual(0);
        atendimento.setDadosColetados(dadosColetados);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return atendimento;
    }
}
