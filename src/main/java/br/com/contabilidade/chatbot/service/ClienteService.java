package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.dto.ClienteDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.exception.RecursoNaoEncontradoException;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    @Transactional
    public ClienteDTO criar(ClienteDTO dto) {
        Cliente cliente = new Cliente();
        cliente.setNome(dto.getNome());
        cliente.setTelefone(dto.getTelefone());
        cliente.setEmail(dto.getEmail());
        cliente.setCpfCnpj(dto.getCpfCnpj());
        cliente.setTipoPessoa(dto.getTipoPessoa());
        cliente.setNomeEmpresa(dto.getNomeEmpresa());
        cliente.setRegimeTributario(dto.getRegimeTributario());
        cliente.setSegmento(dto.getSegmento());
        cliente.setStatus(dto.getStatus() == null || dto.getStatus().isBlank() ? "ATIVO" : dto.getStatus());
        cliente.setCriadoEm(LocalDateTime.now());
        return toDto(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<ClienteDTO> listar() {
        return clienteRepository.findAll().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Cliente buscarEntidade(Long id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente nao encontrado"));
    }

    ClienteDTO toDto(Cliente cliente) {
        ClienteDTO dto = new ClienteDTO();
        dto.setId(cliente.getId());
        dto.setNome(cliente.getNome());
        dto.setTelefone(cliente.getTelefone());
        dto.setEmail(cliente.getEmail());
        dto.setCpfCnpj(cliente.getCpfCnpj());
        dto.setTipoPessoa(cliente.getTipoPessoa());
        dto.setNomeEmpresa(cliente.getNomeEmpresa());
        dto.setRegimeTributario(cliente.getRegimeTributario());
        dto.setSegmento(cliente.getSegmento());
        dto.setStatus(cliente.getStatus());
        dto.setCriadoEm(cliente.getCriadoEm());
        return dto;
    }
}
