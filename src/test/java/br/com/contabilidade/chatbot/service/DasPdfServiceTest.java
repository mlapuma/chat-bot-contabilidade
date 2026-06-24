package br.com.contabilidade.chatbot.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.Cliente;
import br.com.contabilidade.chatbot.entity.StatusAtendimento;
import br.com.contabilidade.chatbot.entity.TipoAtendimento;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class DasPdfServiceTest {

    private final DasPdfService dasPdfService = new DasPdfService(new ObjectMapper());

    @Test
    void deveGerarPdfDaSolicitacaoDeDas() throws Exception {
        Cliente cliente = new Cliente();
        cliente.setNome("Empresa Teste");
        cliente.setTelefone("11999990000");
        cliente.setEmail("contato@empresa.test");

        Atendimento atendimento = new Atendimento();
        atendimento.setId(999L);
        atendimento.setCliente(cliente);
        atendimento.setTipoAtendimento(TipoAtendimento.EMITIR_DAS);
        atendimento.setStatus(StatusAtendimento.NOVO);
        atendimento.setCriadoEm(LocalDateTime.now());
        atendimento.setAtualizadoEm(LocalDateTime.now());

        String caminho = dasPdfService.gerarPdfSolicitacao(atendimento, Map.of(
                "cnpj", "12.345.678/0001-90",
                "mesReferencia", "06/2026",
                "emailEnvio", "financeiro@empresa.test"
        ));

        Path arquivo = Path.of(caminho);
        assertThat(arquivo).exists();
        assertThat(Files.readAllBytes(arquivo)).startsWith("%PDF-1.4".getBytes());
    }
}
