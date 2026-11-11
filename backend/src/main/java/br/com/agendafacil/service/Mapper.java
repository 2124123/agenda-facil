package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.model.*;

/** Conversões entidade -> DTO. */
public final class Mapper {
    private Mapper() {}

    public static UsuarioDto usuario(Usuario u) {
        return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getTelefone(), u.getPerfil());
    }

    public static ServicoDto servico(Servico s) {
        return new ServicoDto(s.getId(), s.getNome(), s.getDescricao(), s.getCategoria(),
                s.getDuracaoMinutos(), s.getPreco(), s.isAtivo());
    }

    public static ProfissionalDto profissional(Profissional p) {
        Usuario u = p.getUsuario();
        return new ProfissionalDto(p.getId(), u.getNome(), u.getEmail(), u.getTelefone(),
                p.getEspecialidade(), p.isAtivo());
    }

    public static AgendamentoDto agendamento(Agendamento a) {
        Usuario cli = a.getCliente().getUsuario();
        Estabelecimento est = a.getProfissional().getEstabelecimento();
        return new AgendamentoDto(a.getId(), a.getServico().getId(), a.getServico().getNome(), a.getServico().getCategoria(),
                a.getProfissional().getId(), a.getProfissional().getUsuario().getNome(),
                a.getCliente().getId(), cli.getNome(), cli.getTelefone(),
                a.getData(), a.getHoraInicio(), a.getHoraFim(), a.getStatus(), a.getValor(),
                est.getNome(), est.getEndereco());
    }

    public static BloqueioDto bloqueio(BloqueioHorario b) {
        return new BloqueioDto(b.getId(), b.getData(), b.getHoraInicio(), b.getHoraFim(), b.getMotivo());
    }

    public static NotificacaoDto notificacao(Notificacao n) {
        return new NotificacaoDto(n.getId(), n.getTipo(), n.getMensagem(), n.isLida(), n.getCriadaEm());
    }
}
