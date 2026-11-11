package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.security.AuthUser;
import br.com.agendafacil.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cliente/agendamentos")
@RequiredArgsConstructor
public class ClienteController {
    private final AgendamentoService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgendamentoDto criar(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody AgendamentoRequest req) {
        return service.criar(u.id(), req);
    }

    /** tipo = proximos | historico */
    @GetMapping
    public List<AgendamentoDto> meus(@AuthenticationPrincipal AuthUser u, @RequestParam(defaultValue = "proximos") String tipo) {
        return service.meus(u.id(), tipo);
    }

    @PutMapping("/{id}/remarcar")
    public AgendamentoDto remarcar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id, @Valid @RequestBody RemarcarRequest req) {
        return service.remarcar(u.id(), id, req);
    }

    @PatchMapping("/{id}/cancelar")
    public AgendamentoDto cancelar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.cancelarPeloCliente(u.id(), id);
    }
}
