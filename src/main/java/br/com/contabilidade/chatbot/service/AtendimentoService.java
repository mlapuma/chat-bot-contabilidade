package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.exception.RecursoNaoEncontradoException;
import br.com.contabilidade.chatbot.repository.AtendimentoRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoService {

    private final AtendimentoRepository atendimentoRepository;

    public AtendimentoService(AtendimentoRepository atendimentoRepository) {
        this.atendimentoRepository = atendimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<AtendimentoDTO> listar() {
        return atendimentoRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public AtendimentoDTO buscar(Long id) {
        return toDto(buscarEntidade(id));
    }

    @Transactional
    public AtendimentoDTO atualizarStatus(Long id, StatusAtendimento status) {
        Atendimento atendimento = buscarEntidade(id);
        atendimento.setStatus(status);
        atendimento.setAtualizadoEm(LocalDateTime.now());
        return toDto(atendimentoRepository.save(atendimento));
    }

    public Atendimento buscarEntidade(Long id) {
        return atendimentoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atendimento nao encontrado"));
    }

    AtendimentoDTO toDto(Atendimento atendimento) {
        AtendimentoDTO dto = new AtendimentoDTO();
        dto.setId(atendimento.getId());
        if (atendimento.getCliente() != null) {
            dto.setClienteId(atendimento.getCliente().getId());
            dto.setNomeCliente(atendimento.getCliente().getNome());
        }
        dto.setTipoAtendimento(atendimento.getTipoAtendimento());
        dto.setStatus(atendimento.getStatus());
        dto.setDadosColetados(atendimento.getDadosColetados());
        dto.setCriadoEm(atendimento.getCriadoEm());
        dto.setAtualizadoEm(atendimento.getAtualizadoEm());
        return dto;
    }
}
