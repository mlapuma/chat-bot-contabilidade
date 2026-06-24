package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.entity.Atendimento;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ManualDasEmissionServiceTest {

    @Mock
    private DasPdfService dasPdfService;

    @Test
    void deveRegistrarEmissaoManualAssistidaComPdfInterno() {
        Atendimento atendimento = new Atendimento();
        atendimento.setId(55L);
        Map<String, String> dados = Map.of("cnpj", "12345678000190");
        when(dasPdfService.gerarPdfSolicitacao(any(Atendimento.class), any()))
                .thenReturn("generated/das/das-atendimento-55.pdf");

        ManualDasEmissionService service = new ManualDasEmissionService(dasPdfService);

        DasEmissionResult result = service.emitir(new DasEmissionRequest(atendimento, dados));

        assertThat(result.modoEmissao()).isEqualTo("MANUAL_ASSISTIDO");
        assertThat(result.statusEmissao()).isEqualTo("AGUARDANDO_EMISSAO_OFICIAL");
        assertThat(result.arquivoPdf()).isEqualTo("generated/das/das-atendimento-55.pdf");
        assertThat(result.urlPdf()).isEqualTo("/api/atendimentos/55/das-pdf");
        assertThat(result.mensagemCliente()).contains("equipe contábil");
        assertThat(result.mensagemCliente()).contains("contador fará a emissão oficial");
        assertThat(result.mensagemCliente()).doesNotContain("/api/");
        verify(dasPdfService).gerarPdfSolicitacao(atendimento, dados);
    }
}
