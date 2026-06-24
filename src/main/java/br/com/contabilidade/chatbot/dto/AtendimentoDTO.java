package br.com.contabilidade.chatbot.dto;

import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import java.time.LocalDateTime;

public class AtendimentoDTO {

    private Long id;
    private Long clienteId;
    private String nomeCliente;
    private String telefoneCliente;
    private TipoAtendimento tipoAtendimento;
    private String departamento;
    private String numeroWhatsAppDestino;
    private String phoneNumberIdDestino;
    private StatusAtendimento status;
    private String dadosColetados;
    private String atendenteResponsavel;
    private LocalDateTime assumidoEm;
    private LocalDateTime criadoEm;
    private LocalDateTime atualizadoEm;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getTelefoneCliente() {
        return telefoneCliente;
    }

    public void setTelefoneCliente(String telefoneCliente) {
        this.telefoneCliente = telefoneCliente;
    }

    public TipoAtendimento getTipoAtendimento() {
        return tipoAtendimento;
    }

    public void setTipoAtendimento(TipoAtendimento tipoAtendimento) {
        this.tipoAtendimento = tipoAtendimento;
    }

    public String getDepartamento() {
        return departamento;
    }

    public void setDepartamento(String departamento) {
        this.departamento = departamento;
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

    public StatusAtendimento getStatus() {
        return status;
    }

    public void setStatus(StatusAtendimento status) {
        this.status = status;
    }

    public String getDadosColetados() {
        return dadosColetados;
    }

    public void setDadosColetados(String dadosColetados) {
        this.dadosColetados = dadosColetados;
    }

    public String getAtendenteResponsavel() {
        return atendenteResponsavel;
    }

    public void setAtendenteResponsavel(String atendenteResponsavel) {
        this.atendenteResponsavel = atendenteResponsavel;
    }

    public LocalDateTime getAssumidoEm() {
        return assumidoEm;
    }

    public void setAssumidoEm(LocalDateTime assumidoEm) {
        this.assumidoEm = assumidoEm;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(LocalDateTime atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
