package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.service.AuthService;
import br.com.agendafacil.service.UsuarioService;
import br.com.agendafacil.security.AuthUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AuthController {
    private final AuthService auth;
    private final UsuarioService usuarios;

    @PostMapping("/api/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @PostMapping("/api/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.registrar(req);
    }

    @GetMapping("/api/me")
    public UsuarioDto me(@AuthenticationPrincipal AuthUser user) {
        return usuarios.me(user.id());
    }

    @PutMapping("/api/me")
    public UsuarioDto atualizar(@AuthenticationPrincipal AuthUser user, @Valid @RequestBody PerfilUpdate req) {
        return usuarios.atualizar(user.id(), req);
    }
}
