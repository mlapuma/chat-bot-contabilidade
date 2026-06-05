package br.com.contabilidade.chatbot.service;

import org.springframework.stereotype.Service;

@Service
public class WhatsAppService {

    /*
     * Integracao futura com WhatsApp Business Cloud API:
     * - configurar token permanente ou fluxo OAuth da Meta;
     * - validar webhook GET com hub.challenge;
     * - receber mensagens via webhook POST;
     * - mapear telefone do remetente para Cliente;
     * - chamar ChatbotService com o texto recebido;
     * - responder usando o endpoint /{phone-number-id}/messages da Graph API;
     * - registrar payloads sem gravar tokens ou dados sensiveis em logs.
     */
}
