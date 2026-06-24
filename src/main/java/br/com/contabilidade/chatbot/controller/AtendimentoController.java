package br.com.contabilidade.chatbot.controller;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.dto.AssumirAtendimentoDTO;
import br.com.contabilidade.chatbot.dto.ResponderAtendimentoDTO;
import br.com.contabilidade.chatbot.dto.RespostaAtendenteDTO;
import br.com.contabilidade.chatbot.dto.StatusUpdateDTO;
import br.com.contabilidade.chatbot.entity.Atendimento;
import br.com.contabilidade.chatbot.service.AtendimentoService;
import br.com.contabilidade.chatbot.service.DasPdfService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;
    private final DasPdfService dasPdfService;

    public AtendimentoController(AtendimentoService atendimentoService, DasPdfService dasPdfService) {
        this.atendimentoService = atendimentoService;
        this.dasPdfService = dasPdfService;
    }

    @GetMapping
    public List<AtendimentoDTO> listar(@RequestParam(required = false) String departamento) {
        return atendimentoService.listar(departamento);
    }

    @GetMapping("/{id}")
    public AtendimentoDTO buscar(@PathVariable Long id) {
        return atendimentoService.buscar(id);
    }

    @GetMapping("/{id}/das-pdf")
    public ResponseEntity<Resource> baixarDasPdf(@PathVariable Long id) {
        Atendimento atendimento = atendimentoService.buscarEntidade(id);
        Resource pdf = dasPdfService.carregarPdf(atendimento);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + dasPdfService.nomeArquivo(atendimento) + "\"")
                .body(pdf);
    }

    @PutMapping("/{id}/status")
    public AtendimentoDTO atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateDTO dto) {
        return atendimentoService.atualizarStatus(id, dto.getStatus());
    }

    @PostMapping("/{id}/assumir")
    public AtendimentoDTO assumir(@PathVariable Long id, @Valid @RequestBody AssumirAtendimentoDTO dto) {
        return atendimentoService.assumir(id, dto.getAtendente());
    }

    @PostMapping("/{id}/liberar")
    public AtendimentoDTO liberar(@PathVariable Long id) {
        return atendimentoService.liberar(id);
    }

    @PostMapping("/{id}/responder")
    public RespostaAtendenteDTO responder(@PathVariable Long id, @Valid @RequestBody ResponderAtendimentoDTO dto) {
        return atendimentoService.responder(id, dto.getAtendente(), dto.getMensagem());
    }
}
