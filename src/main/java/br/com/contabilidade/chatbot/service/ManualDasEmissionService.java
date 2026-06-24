package br.com.contabilidade.chatbot.service;

import org.springframework.stereotype.Service;

@Service
public class ManualDasEmissionService implements DasEmissionService {

    private final DasPdfService dasPdfService;

    public ManualDasEmissionService(DasPdfService dasPdfService) {
        this.dasPdfService = dasPdfService;
    }

    @Override
    public DasEmissionResult emitir(DasEmissionRequest request) {
        String arquivoPdf = dasPdfService.gerarPdfSolicitacao(request.atendimento(), request.dados());
        String urlPdf = "/api/atendimentos/%d/das-pdf".formatted(request.atendimento().getId());
        return new DasEmissionResult(
                "MANUAL_ASSISTIDO",
                "AGUARDANDO_EMISSAO_OFICIAL",
                arquivoPdf,
                urlPdf,
                "O PDF interno da solicitação do DAS foi gerado para uso da equipe contábil. "
                        + "O contador fará a emissão oficial e entrará em contato com você."
        );
    }
}
