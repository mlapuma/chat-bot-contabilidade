package br.com.contabilidade.chatbot.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class WhatsAppTestRequestDTO {

    @NotBlank
    @Size(max = 120)
    private String nome;

    @NotBlank
    @Size(max = 30)
    private String telefone;

    @NotBlank
    @Size(max = 1000)
    private String mensagem;

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
