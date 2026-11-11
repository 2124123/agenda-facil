package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.PerfilUpdate;
import br.com.agendafacil.dto.Dtos.UsuarioDto;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.Usuario;
import br.com.agendafacil.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsuarioService {
    private final UsuarioRepository usuarios;

    @Transactional(readOnly = true)
    public UsuarioDto me(Long id) {
        return Mapper.usuario(buscar(id));
    }

    @Transactional
    public UsuarioDto atualizar(Long id, PerfilUpdate req) {
        Usuario u = buscar(id);
        u.setNome(req.nome().trim());
        u.setTelefone(req.telefone() == null || req.telefone().isBlank() ? null : req.telefone().trim());
        return Mapper.usuario(u);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Usuário não encontrado"));
    }
}
