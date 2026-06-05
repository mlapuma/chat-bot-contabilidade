package br.com.contabilidade.chatbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WebChatService implements CanalMensagemService {

    private static final Logger log = LoggerFactory.getLogger(WebChatService.class);

    @Override
    public void enviarMensagem(String destino, String mensagem) {
        log.info("Mensagem enviada pelo WebChat para destino={}", destino);
    }
}
