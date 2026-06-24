package br.com.contabilidade.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ChatRequestDTO {

    private Long clienteId;

    @NotBlank
    @Size(max = 1000)
    private String mensagem;

    private String numeroWhatsAppDestino;
    private String phoneNumberIdDestino;

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public String getNumeroWhatsAppDestino() {
        return numeroWhatsAppDestino;
    }

    public void setNumeroWhatsAppDestino(String numeroWhatsAppDestino) {
        this.numeroWhatsAppDestino = numeroWhatsAppDestino;
    }

    public String getPhoneNumberIdDestino() {
        return phoneNumberIdDestino;
    }

    public void setPhoneNumberIdDestino(String phoneNumberIdDestino) {
        this.phoneNumberIdDestino = phoneNumberIdDestino;
    }
}
