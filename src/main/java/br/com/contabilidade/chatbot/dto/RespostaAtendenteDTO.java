package br.com.contabilidade.chatbot.dto;

public class RespostaAtendenteDTO {

    private AtendimentoDTO atendimento;
    private boolean enviadoWhatsApp;
    private String mensagem;

    public RespostaAtendenteDTO(AtendimentoDTO atendimento, boolean enviadoWhatsApp, String mensagem) {
        this.atendimento = atendimento;
        this.enviadoWhatsApp = enviadoWhatsApp;
        this.mensagem = mensagem;
    }

    public AtendimentoDTO getAtendimento() {
        return atendimento;
    }

    public void setAtendimento(AtendimentoDTO atendimento) {
        this.atendimento = atendimento;
    }

    public boolean isEnviadoWhatsApp() {
        return enviadoWhatsApp;
    }

    public void setEnviadoWhatsApp(boolean enviadoWhatsApp) {
        this.enviadoWhatsApp = enviadoWhatsApp;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
