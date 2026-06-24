package br.com.contabilidade.chatbot;

import br.com.contabilidade.chatbot.config.WhatsAppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(WhatsAppProperties.class)
public class ChatbotContabilidadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(ChatbotContabilidadeApplication.class, args);
    }
}
