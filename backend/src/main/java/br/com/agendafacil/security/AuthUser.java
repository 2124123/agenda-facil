package br.com.agendafacil.security;

import br.com.agendafacil.model.Perfil;

/** Usuário autenticado extraído do token JWT. */
public record AuthUser(Long id, Perfil perfil) {}
