package br.com.contabilidade.chatbot.dto;

import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import jakarta.validation.constraints.NotNull;

public class StatusUpdateDTO {

    @NotNull
    private StatusAtendimento status;

    public StatusAtendimento getStatus() {
        return status;
    }

    public void setStatus(StatusAtendimento status) {
        this.status = status;
    }
}
