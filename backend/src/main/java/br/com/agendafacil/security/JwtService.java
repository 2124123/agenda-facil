package br.com.agendafacil.security;

import br.com.agendafacil.model.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey chave;
    private final long horas;

    public JwtService(@Value("${agenda.jwt-secret}") String segredo,
                      @Value("${agenda.jwt-horas:24}") long horas) {
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.horas = horas;
    }

    public String gerar(Usuario u) {
        Date agora = new Date();
        return Jwts.builder()
                .subject(String.valueOf(u.getId()))
                .claim("perfil", u.getPerfil().name())
                .issuedAt(agora)
                .expiration(new Date(agora.getTime() + horas * 3_600_000L))
                .signWith(chave)
                .compact();
    }

    public Long extrairId(String token) {
        Claims c = Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
        return Long.valueOf(c.getSubject());
    }
}
