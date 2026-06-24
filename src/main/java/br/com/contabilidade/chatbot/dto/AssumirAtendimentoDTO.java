package br.com.contabilidade.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AssumirAtendimentoDTO {

    @NotBlank
    @Size(max = 120)
    private String atendente;

    public String getAtendente() {
        return atendente;
    }

    public void setAtendente(String atendente) {
        this.atendente = atendente;
    }
}
