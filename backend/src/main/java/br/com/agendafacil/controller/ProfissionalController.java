package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.security.AuthUser;
import br.com.agendafacil.service.AgendamentoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/profissional")
@RequiredArgsConstructor
public class ProfissionalController {
    private final AgendamentoService service;

    @GetMapping("/agenda")
    public AgendaDto agenda(@AuthenticationPrincipal AuthUser u,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                            @RequestParam(defaultValue = "1") int dias) {
        return service.agenda(u.id(), data, dias);
    }

    @PatchMapping("/agendamentos/{id}/confirmar")
    public AgendamentoDto confirmar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.confirmar(u.id(), id);
    }

    @PatchMapping("/agendamentos/{id}/realizar")
    public AgendamentoDto realizar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.registrarRealizado(u.id(), id);
    }

    @PatchMapping("/agendamentos/{id}/cancelar")
    public AgendamentoDto cancelar(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        return service.cancelarPeloProfissional(u.id(), id);
    }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioDto bloquear(@AuthenticationPrincipal AuthUser u, @Valid @RequestBody BloqueioRequest req) {
        return service.bloquear(u.id(), req);
    }

    @DeleteMapping("/bloqueios/{id}")
    public ResponseEntity<Void> removerBloqueio(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        service.removerBloqueio(u.id(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/clientes")
    public List<ClienteResumoDto> clientes(@AuthenticationPrincipal AuthUser u) {
        return service.clientesAtendidos(u.id());
    }
}
