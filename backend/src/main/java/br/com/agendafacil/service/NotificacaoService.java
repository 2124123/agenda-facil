package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.NotificacaoDto;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.NotificacaoRepository;
import br.com.agendafacil.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificacaoService {
    private final NotificacaoRepository repo;
    private final UsuarioRepository usuarios;

    @Transactional
    public void notificar(Usuario destino, TipoNotificacao tipo, String mensagem) {
        Notificacao n = new Notificacao();
        n.setUsuario(destino);
        n.setTipo(tipo);
        n.setMensagem(mensagem);
        repo.save(n);
    }

    @Transactional
    public void notificarAdmins(TipoNotificacao tipo, String mensagem) {
        usuarios.findByPerfil(Perfil.ADMINISTRADOR).stream()
                .filter(u -> u.getStatus() == StatusUsuario.ATIVO)
                .forEach(u -> notificar(u, tipo, mensagem));
    }

    @Transactional(readOnly = true)
    public List<NotificacaoDto> listar(Long usuarioId) {
        return repo.findTop50ByUsuarioIdOrderByCriadaEmDesc(usuarioId).stream().map(Mapper::notificacao).toList();
    }

    @Transactional
    public void marcarLida(Long usuarioId, Long id) {
        Notificacao n = repo.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Notificação não encontrada"));
        if (!n.getUsuario().getId().equals(usuarioId)) throw ApiException.proibido("Notificação de outro usuário");
        n.setLida(true);
    }

    @Transactional
    public void marcarTodasLidas(Long usuarioId) {
        repo.findByUsuarioIdAndLidaFalse(usuarioId).forEach(n -> n.setLida(true));
    }
}
