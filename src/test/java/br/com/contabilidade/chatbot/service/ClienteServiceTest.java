package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import br.com.contabilidade.chatbot.dto.ClienteDTO;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.TipoPessoa;
import br.com.contabilidade.chatbot.repository.ClienteRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    void deveCriarClienteComStatusPadrao() {
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(invocation -> {
            Cliente cliente = invocation.getArgument(0);
            cliente.setId(1L);
            return cliente;
        });

        ClienteDTO dto = new ClienteDTO();
        dto.setNome("Maria Silva");
        dto.setTelefone("11999990000");
        dto.setEmail("maria@email.com");
        dto.setCpfCnpj("12345678901");
        dto.setTipoPessoa(TipoPessoa.FISICA);

        ClienteDTO criado = clienteService.criar(dto);

        assertThat(criado.getId()).isEqualTo(1L);
        assertThat(criado.getStatus()).isEqualTo("ATIVO");
        assertThat(criado.getCriadoEm()).isNotNull();
    }

    @Test
    void deveListarClientes() {
        Cliente cliente = new Cliente();
        cliente.setId(2L);
        cliente.setNome("Empresa Exemplo");
        cliente.setTelefone("1133334444");
        cliente.setEmail("contato@empresa.com");
        cliente.setCpfCnpj("11222333000144");
        cliente.setTipoPessoa(TipoPessoa.JURIDICA);
        cliente.setStatus("ATIVO");

        when(clienteRepository.findAll()).thenReturn(List.of(cliente));

        assertThat(clienteService.listar()).hasSize(1);
    }
}
