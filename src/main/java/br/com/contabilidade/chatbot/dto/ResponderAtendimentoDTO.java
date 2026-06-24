package br.com.contabilidade.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResponderAtendimentoDTO {

    @NotBlank
    @Size(max = 120)
    private String atendente;

    @NotBlank
    @Size(max = 4000)
    private String mensagem;

    public String getAtendente() {
        return atendente;
    }

    public void setAtendente(String atendente) {
        this.atendente = atendente;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
