package br.com.contabilidade.chatbot.dto;

public class WhatsAppWebhookResultDTO {

    private String telefoneCliente;
    private String mensagemRecebida;
    private String respostaBot;
    private Long clienteId;
    private Long atendimentoId;
    private boolean respostaEnviada;

    public WhatsAppWebhookResultDTO(String telefoneCliente,
                                    String mensagemRecebida,
                                    String respostaBot,
                                    Long clienteId,
                                    Long atendimentoId,
                                    boolean respostaEnviada) {
        this.telefoneCliente = telefoneCliente;
        this.mensagemRecebida = mensagemRecebida;
        this.respostaBot = respostaBot;
        this.clienteId = clienteId;
        this.atendimentoId = atendimentoId;
        this.respostaEnviada = respostaEnviada;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public String getMensagemRecebida() {
        return mensagemRecebida;
    }

    public String getRespostaBot() {
        return respostaBot;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public Long getAtendimentoId() {
        return atendimentoId;
    }

    public boolean isRespostaEnviada() {
        return respostaEnviada;
    }
}
