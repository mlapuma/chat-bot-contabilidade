package br.com.contabilidade.chatbot.controller;

import br.com.contabilidade.chatbot.dto.AtendimentoDTO;
import br.com.contabilidade.chatbot.dto.StatusUpdateDTO;
import br.com.contabilidade.chatbot.service.AtendimentoService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService atendimentoService;

    public AtendimentoController(AtendimentoService atendimentoService) {
        this.atendimentoService = atendimentoService;
    }

    @GetMapping
    public List<AtendimentoDTO> listar() {
        return atendimentoService.listar();
    }

    @GetMapping("/{id}")
    public AtendimentoDTO buscar(@PathVariable Long id) {
        return atendimentoService.buscar(id);
    }

    @PutMapping("/{id}/status")
    public AtendimentoDTO atualizarStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateDTO dto) {
        return atendimentoService.atualizarStatus(id, dto.getStatus());
    }
}
