package br.com.contabilidade.chatbot.dto;

import br.com.contabilidade.chatbot.entity.DirecaoMensagem;
import java.time.LocalDateTime;

public class MensagemChatDTO {

    private Long id;
    private Long atendimentoId;
    private DirecaoMensagem direcao;
    private String conteudo;
    private LocalDateTime criadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAtendimentoId() {
        return atendimentoId;
    }

    public void setAtendimentoId(Long atendimentoId) {
        this.atendimentoId = atendimentoId;
    }

    public DirecaoMensagem getDirecao() {
        return direcao;
    }

    public void setDirecao(DirecaoMensagem direcao) {
        this.direcao = direcao;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }
}
