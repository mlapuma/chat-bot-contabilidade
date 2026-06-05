package br.com.contabilidade.chatbot.dto;

import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;

public class ChatResponseDTO {

    private String resposta;
    private Long atendimentoId;
    private TipoAtendimento tipoAtendimento;
    private StatusAtendimento status;

    public ChatResponseDTO(String resposta, Long atendimentoId, TipoAtendimento tipoAtendimento, StatusAtendimento status) {
        this.resposta = resposta;
        this.atendimentoId = atendimentoId;
        this.tipoAtendimento = tipoAtendimento;
        this.status = status;
    }

    public String getResposta() {
        return resposta;
    }

    public Long getAtendimentoId() {
        return atendimentoId;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public StatusAtendimento getStatus() {
        return status;
    }
}
