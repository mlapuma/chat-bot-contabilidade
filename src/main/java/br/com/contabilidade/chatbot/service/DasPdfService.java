package br.com.contabilidade.chatbot.service;

import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.entity.Cliente;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class DasPdfService {

    private static final Charset PDF_CHARSET = Charset.forName("windows-1252");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Path DIRETORIO_DAS = Path.of("generated", "das");

    private final ObjectMapper objectMapper;

    public DasPdfService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String gerarPdfSolicitacao(Atendimento atendimento, Map<String, String> dados) {
        try {
            Files.createDirectories(DIRETORIO_DAS);
            String nomeArquivo = "das-atendimento-%d.pdf".formatted(atendimento.getId());
            Path arquivo = DIRETORIO_DAS.resolve(nomeArquivo);
            Files.write(arquivo, montarPdf(atendimento, dados));
            return arquivo.toAbsolutePath().toString();
        } catch (IOException ex) {
            throw new IllegalStateException("Não foi possível gerar o PDF da solicitação de DAS", ex);
        }
    }

    public Resource carregarPdf(Atendimento atendimento) {
        Map<String, String> dados = lerDados(atendimento.getDadosColetados());
        String caminho = dados.get("arquivoDasPdf");
        if (caminho == null || caminho.isBlank()) {
            throw new IllegalStateException("PDF da solicitação de DAS não encontrado para este atendimento");
        }

        Path arquivo = Path.of(caminho).normalize();
        if (!Files.exists(arquivo)) {
            throw new IllegalStateException("Arquivo PDF da solicitação de DAS não existe no disco");
        }
        return new PathResource(arquivo);
    }

    public String nomeArquivo(Atendimento atendimento) {
        return "das-atendimento-%d.pdf".formatted(atendimento.getId());
    }

    private byte[] montarPdf(Atendimento atendimento, Map<String, String> dados) throws IOException {
        List<String> linhas = linhasDoDocumento(atendimento, dados);
        StringBuilder conteudo = new StringBuilder();
        conteudo.append("BT\n");
        conteudo.append("/F1 16 Tf\n");
        conteudo.append("50 790 Td\n");
        conteudo.append("(").append(escapar("Solicitação de emissão de DAS")).append(") Tj\n");
        conteudo.append("/F1 10 Tf\n");
        conteudo.append("0 -24 Td\n");
        for (String linha : linhas) {
            conteudo.append("(").append(escapar(linha)).append(") Tj\n");
            conteudo.append("0 -16 Td\n");
        }
        conteudo.append("ET\n");

        byte[] stream = conteudo.toString().getBytes(PDF_CHARSET);
        List<byte[]> objetos = new ArrayList<>();
        objetos.add("<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.US_ASCII));
        objetos.add("<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.US_ASCII));
        objetos.add("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>".getBytes(StandardCharsets.US_ASCII));
        objetos.add("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>".getBytes(StandardCharsets.US_ASCII));
        objetos.add(("<< /Length " + stream.length + " >>\nstream\n").getBytes(StandardCharsets.US_ASCII));

        ByteArrayOutputStream pdf = new ByteArrayOutputStream();
        pdf.write("%PDF-1.4\n".getBytes(StandardCharsets.US_ASCII));
        List<Integer> offsets = new ArrayList<>();
        for (int i = 0; i < objetos.size(); i++) {
            offsets.add(pdf.size());
            pdf.write(("%d 0 obj\n".formatted(i + 1)).getBytes(StandardCharsets.US_ASCII));
            pdf.write(objetos.get(i));
            if (i == 4) {
                pdf.write(stream);
                pdf.write("\nendstream".getBytes(StandardCharsets.US_ASCII));
            }
            pdf.write("\nendobj\n".getBytes(StandardCharsets.US_ASCII));
        }

        int xref = pdf.size();
        pdf.write(("xref\n0 " + (objetos.size() + 1) + "\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write("0000000000 65535 f \n".getBytes(StandardCharsets.US_ASCII));
        for (Integer offset : offsets) {
            pdf.write(("%010d 00000 n \n".formatted(offset)).getBytes(StandardCharsets.US_ASCII));
        }
        pdf.write(("trailer\n<< /Size " + (objetos.size() + 1) + " /Root 1 0 R >>\n").getBytes(StandardCharsets.US_ASCII));
        pdf.write(("startxref\n" + xref + "\n%%EOF").getBytes(StandardCharsets.US_ASCII));
        return pdf.toByteArray();
    }

    private List<String> linhasDoDocumento(Atendimento atendimento, Map<String, String> dados) {
        Cliente cliente = atendimento.getCliente();
        return List.of(
                "Documento gerado automaticamente pelo chatbot para validação interna.",
                "Atendimento: " + atendimento.getId(),
                "Data da solicitação: " + LocalDateTime.now().format(DATA_HORA),
                "Cliente: " + valor(cliente == null ? null : cliente.getNome()),
                "Telefone: " + valor(cliente == null ? null : cliente.getTelefone()),
                "E-mail do cadastro: " + valor(cliente == null ? null : cliente.getEmail()),
                "CNPJ informado: " + valor(dados.get("cnpj")),
                "Mês de referência: " + valor(dados.get("mesReferencia")),
                "E-mail para envio: " + valor(dados.get("emailEnvio")),
                "Status do atendimento: " + atendimento.getStatus(),
                "Observação: este PDF registra a solicitação. A emissão fiscal real do DAS",
                "deve ser confirmada pelo contador nos sistemas oficiais."
        );
    }

    private String valor(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor;
    }

    private String escapar(String texto) {
        String compativel = Normalizer.normalize(texto, Normalizer.Form.NFC);
        return compativel
                .replace("\\", "\\\\")
                .replace("(", "\\(")
                .replace(")", "\\)");
    }

    private Map<String, String> lerDados(String json) {
        try {
            if (json == null || json.isBlank()) {
                return new LinkedHashMap<>();
            }
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }
}
