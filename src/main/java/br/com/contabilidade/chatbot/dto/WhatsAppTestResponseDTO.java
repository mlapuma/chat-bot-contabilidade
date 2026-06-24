package br.com.contabilidade.chatbot.dto;

import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;

public class WhatsAppTestResponseDTO {

    private Long clienteId;
    private String canal;
    private String telefone;
    private String resposta;
    private Long atendimentoId;
    private TipoAtendimento tipoAtendimento;
    private StatusAtendimento status;

    public WhatsAppTestResponseDTO(Long clienteId,
                                   String canal,
                                   String telefone,
                                   String resposta,
                                   Long atendimentoId,
                                   TipoAtendimento tipoAtendimento,
                                   StatusAtendimento status) {
        this.clienteId = clienteId;
        this.canal = canal;
        this.telefone = telefone;
        this.resposta = resposta;
        this.atendimentoId = atendimentoId;
        this.tipoAtendimento = tipoAtendimento;
        this.status = status;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public String getCanal() {
        return canal;
    }

    public String getTelefone() {
        return telefone;
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
