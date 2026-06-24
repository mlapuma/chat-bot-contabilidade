package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.entity.Atendimento;
import java.util.Map;

public record DasEmissionRequest(Atendimento atendimento, Map<String, String> dados) {
}
