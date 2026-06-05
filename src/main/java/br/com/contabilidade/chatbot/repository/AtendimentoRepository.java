package br.com.contabilidade.chatbot.repository;

import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AtendimentoRepository extends JpaRepository<Atendimento, Long> {

    Optional<Atendimento> findFirstByClienteIdAndStatusInOrderByAtualizadoEmDesc(Long clienteId, Collection<StatusAtendimento> status);
}
