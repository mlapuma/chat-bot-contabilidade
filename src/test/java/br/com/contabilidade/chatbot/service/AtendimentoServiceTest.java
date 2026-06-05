package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import java.time.LocalDateTime;
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
}
