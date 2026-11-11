package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.ClienteRepository;
import br.com.agendafacil.repository.EstabelecimentoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import br.com.agendafacil.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final EstabelecimentoRepository estabelecimentos;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        ApiException invalido = new ApiException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos");
        Usuario u = usuarios.findByEmailIgnoreCase(req.email().trim()).orElseThrow(() -> invalido);
        if (!encoder.matches(req.senha(), u.getSenha())) throw invalido;
        if (u.getStatus() != StatusUsuario.ATIVO) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Usuário inativo. Procure o administrador do estabelecimento.");
        }
        if (req.perfil() != null && req.perfil() != u.getPerfil()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Este usuário não possui o perfil selecionado");
        }
        return resposta(u);
    }

    /** RF001 / UC001: autocadastro de cliente. */
    @Transactional
    public AuthResponse registrar(RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) throw ApiException.conflito("E-mail já cadastrado");
        Usuario u = new Usuario();
        u.setNome(req.nome().trim());
        u.setEmail(email);
        u.setTelefone(req.telefone() == null ? null : req.telefone().trim());
        u.setSenha(encoder.encode(req.senha()));
        u.setPerfil(Perfil.CLIENTE);
        u = usuarios.save(u);
        Cliente c = new Cliente();
        c.setUsuario(u);
        clientes.save(c);
        return resposta(u);
    }

    private AuthResponse resposta(Usuario u) {
        String estab = estabelecimentos.findAll().stream().findFirst().map(Estabelecimento::getNome).orElse("Agenda Fácil");
        return new AuthResponse(jwt.gerar(u), Mapper.usuario(u), estab);
    }
}
