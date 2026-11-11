package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.NotificacaoDto;
import br.com.agendafacil.security.AuthUser;
import br.com.agendafacil.service.NotificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {
    private final NotificacaoService service;

    @GetMapping
    public List<NotificacaoDto> listar(@AuthenticationPrincipal AuthUser u) {
        return service.listar(u.id());
    }

    @PatchMapping("/{id}/lida")
    public ResponseEntity<Void> lida(@AuthenticationPrincipal AuthUser u, @PathVariable Long id) {
        service.marcarLida(u.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/lidas")
    public ResponseEntity<Void> todasLidas(@AuthenticationPrincipal AuthUser u) {
        service.marcarTodasLidas(u.id());
        return ResponseEntity.noContent().build();
    }
}
