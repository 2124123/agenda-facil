package br.com.agendafacil.security;

import br.com.agendafacil.model.StatusUsuario;
import br.com.agendafacil.repository.UsuarioRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UsuarioRepository usuarios;

    public JwtAuthFilter(JwtService jwt, UsuarioRepository usuarios) {
        this.jwt = jwt;
        this.usuarios = usuarios;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Long id = jwt.extrairId(header.substring(7));
                usuarios.findById(id)
                        .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                        .ifPresent(u -> {
                            var auth = new UsernamePasswordAuthenticationToken(
                                    new AuthUser(u.getId(), u.getPerfil()), null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getPerfil().name())));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException ignored) {
                // token inválido/expirado: segue sem autenticação e a cadeia responde 401
            }
        }
        chain.doFilter(req, res);
    }
}
