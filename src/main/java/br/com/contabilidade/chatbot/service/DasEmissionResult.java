package br.com.contabilidade.chatbot.service;

public record DasEmissionResult(
        String modoEmissao,
        String statusEmissao,
        String arquivoPdf,
        String urlPdf,
        String mensagemCliente
) {
}
