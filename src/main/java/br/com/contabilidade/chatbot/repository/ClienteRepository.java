package br.com.contabilidade.chatbot.repository;

import br.com.contabilidade.chatbot.entity.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
