package br.com.contabilidade.chatbot.repository;

import br.com.contabilidade.chatbot.entity.MensagemChat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByClienteIdOrderByCriadoEmAsc(Long clienteId);
}
