package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AgendamentoService {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Set<StatusAgendamento> ABERTOS = EnumSet.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO);

    private final AgendamentoRepository agendamentos;
    private final ClienteRepository clientes;
    private final ProfissionalRepository profissionais;
    private final ServicoRepository servicos;
    private final BloqueioHorarioRepository bloqueios;
    private final DisponibilidadeService disponibilidade;
    private final NotificacaoService notificacoes;
    private final Clock clock;

    @Value("${agenda.auto-confirmar:true}")
    private boolean autoConfirmar;

    // =============================================================== CLIENTE

    /** RF004. RN001: só cliente cadastrado agenda. RN002: sem conflito para o mesmo profissional. */
    @Transactional
    public AgendamentoDto criar(Long usuarioId, AgendamentoRequest r) {
        Cliente cliente = clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.proibido("Cadastre-se como cliente para realizar agendamentos"));
        Servico servico = servicos.findById(r.servicoId()).filter(Servico::isAtivo)
                .orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado ou inativo"));
        Profissional prof = profissionais.findByIdForUpdate(r.profissionalId())
                .orElseThrow(() -> ApiException.naoEncontrado("Profissional não encontrado"));
        if (!prof.isAtivo()) throw ApiException.conflito("Profissional indisponível");
        if (!prof.getEspecialidade().equalsIgnoreCase(servico.getCategoria())) {
            throw ApiException.invalido("Este profissional não realiza o serviço selecionado");
        }
        LocalTime fim = r.horaInicio().plusMinutes(servico.getDuracaoMinutos());
        disponibilidade.validarOuFalhar(prof.getId(), r.data(), r.horaInicio(), fim, null);

        Agendamento a = new Agendamento();
        a.setCliente(cliente);
        a.setProfissional(prof);
        a.setServico(servico);
        a.setData(r.data());
        a.setHoraInicio(r.horaInicio());
        a.setHoraFim(fim);
        a.setValor(servico.getPreco());
        a.setStatus(statusInicial());
        a = agendamentos.save(a);

        String msg = "Novo agendamento: " + cliente.getUsuario().getNome() + " agendou " + servico.getNome()
                + " para " + a.getData().format(DIA) + " às " + a.getHoraInicio().format(HORA);
        notificacoes.notificar(prof.getUsuario(), TipoNotificacao.NOVO_AGENDAMENTO, msg);
        notificacoes.notificarAdmins(TipoNotificacao.NOVO_AGENDAMENTO, msg + " com " + prof.getUsuario().getNome());
        if (a.getStatus() == StatusAgendamento.CONFIRMADO) {
            notificacoes.notificar(cliente.getUsuario(), TipoNotificacao.CONFIRMACAO,
                    "Agendamento confirmado: " + servico.getNome() + " com " + prof.getUsuario().getNome()
                            + " em " + a.getData().format(DIA) + " às " + a.getHoraInicio().format(HORA));
        }
        return Mapper.agendamento(a);
    }

    /** RF005 / RN003. */
    @Transactional
    public AgendamentoDto remarcar(Long usuarioId, Long id, RemarcarRequest r) {
        Agendamento a = buscar(id);
        exigirDonoCliente(a, usuarioId);
        exigirAberto(a);
        Profissional prof = profissionais.findByIdForUpdate(a.getProfissional().getId())
                .orElseThrow(() -> ApiException.naoEncontrado("Profissional não encontrado"));
        LocalTime fim = r.horaInicio().plusMinutes(a.getServico().getDuracaoMinutos());
        disponibilidade.validarOuFalhar(prof.getId(), r.data(), r.horaInicio(), fim, a.getId());
        a.setData(r.data());
        a.setHoraInicio(r.horaInicio());
        a.setHoraFim(fim);
        a.setStatus(statusInicial());
        a.setLembreteEnviado(false);
        String msg = "Remarcação: " + a.getCliente().getUsuario().getNome() + " remarcou " + a.getServico().getNome()
                + " para " + a.getData().format(DIA) + " às " + a.getHoraInicio().format(HORA);
        notificacoes.notificar(prof.getUsuario(), TipoNotificacao.REMARCACAO, msg);
        return Mapper.agendamento(a);
    }

    /** RF005 / RN003. */
    @Transactional
    public AgendamentoDto cancelarPeloCliente(Long usuarioId, Long id) {
        Agendamento a = buscar(id);
        exigirDonoCliente(a, usuarioId);
        exigirAberto(a);
        a.setStatus(StatusAgendamento.CANCELADO);
        String msg = "Cancelamento: " + a.getCliente().getUsuario().getNome() + " cancelou " + a.getServico().getNome()
                + " de " + a.getData().format(DIA) + " às " + a.getHoraInicio().format(HORA);
        notificacoes.notificar(a.getProfissional().getUsuario(), TipoNotificacao.CANCELAMENTO, msg);
        notificacoes.notificarAdmins(TipoNotificacao.CANCELAMENTO, msg);
        return Mapper.agendamento(a);
    }

    @Transactional(readOnly = true)
    public List<AgendamentoDto> meus(Long usuarioId, String tipo) {
        Cliente cliente = clientes.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.proibido("Cadastro de cliente não encontrado"));
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Agendamento> todos = agendamentos.findByClienteIdOrderByDataDescHoraInicioDesc(cliente.getId());
        boolean historico = "historico".equalsIgnoreCase(tipo);
        return todos.stream()
                .filter(a -> historico != (ABERTOS.contains(a.getStatus()) && !a.getData().atTime(a.getHoraFim()).isBefore(agora)))
                .sorted(historico
                        ? Comparator.comparing(Agendamento::getData).thenComparing(Agendamento::getHoraInicio).reversed()
                        : Comparator.comparing(Agendamento::getData).thenComparing(Agendamento::getHoraInicio))
                .map(Mapper::agendamento).toList();
    }

    // =============================================================== PROFISSIONAL

    /** RF006 / RN004: apenas a própria agenda. */
    @Transactional(readOnly = true)
    public AgendaDto agenda(Long usuarioId, LocalDate data, int dias) {
        Profissional p = profissionalDoUsuario(usuarioId);
        LocalDate fim = data.plusDays(Math.max(1, Math.min(dias, 31)) - 1L);
        List<AgendamentoDto> ags = agendamentos
                .findByProfissionalIdAndDataBetweenOrderByDataAscHoraInicioAsc(p.getId(), data, fim).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .map(Mapper::agendamento).toList();
        List<BloqueioDto> blq = bloqueios
                .findByProfissionalIdAndDataBetweenOrderByDataAscHoraInicioAsc(p.getId(), data, fim).stream()
                .map(Mapper::bloqueio).toList();
        return new AgendaDto(ags, blq);
    }

    /** RF007. */
    @Transactional
    public AgendamentoDto confirmar(Long usuarioId, Long id) {
        Agendamento a = buscarDoProfissional(usuarioId, id);
        if (a.getStatus() != StatusAgendamento.PENDENTE) throw ApiException.conflito("Apenas agendamentos pendentes podem ser confirmados");
        a.setStatus(StatusAgendamento.CONFIRMADO);
        notificacoes.notificar(a.getCliente().getUsuario(), TipoNotificacao.CONFIRMACAO,
                "Agendamento confirmado: " + a.getServico().getNome() + " com " + a.getProfissional().getUsuario().getNome()
                        + " em " + a.getData().format(DIA) + " às " + a.getHoraInicio().format(HORA));
        return Mapper.agendamento(a);
    }

    /** RF007 / RN006: só registra como realizado se existir agendamento confirmado, e não antes do dia do atendimento. */
    @Transactional
    public AgendamentoDto registrarRealizado(Long usuarioId, Long id) {
        Agendamento a = buscarDoProfissional(usuarioId, id);
        if (a.getStatus() == StatusAgendamento.PENDENTE) throw ApiException.conflito("Confirme o agendamento antes de registrar o atendimento");
        if (a.getStatus() != StatusAgendamento.CONFIRMADO) throw ApiException.conflito("Este agendamento não pode ser registrado como realizado");
        if (a.getData().isAfter(LocalDate.now(clock))) throw ApiException.conflito("O atendimento ainda não ocorreu");
        a.setStatus(StatusAgendamento.REALIZADO);
        return Mapper.agendamento(a);
    }

    @Transactional
    public AgendamentoDto cancelarPeloProfissional(Long usuarioId, Long id) {
        Agendamento a = buscarDoProfissional(usuarioId, id);
        exigirAberto(a);
        a.setStatus(StatusAgendamento.CANCELADO);
        notificacoes.notificar(a.getCliente().getUsuario(), TipoNotificacao.CANCELAMENTO,
                "Cancelamento: seu agendamento de " + a.getServico().getNome() + " em " + a.getData().format(DIA)
                        + " às " + a.getHoraInicio().format(HORA) + " foi cancelado pelo profissional");
        return Mapper.agendamento(a);
    }

    /** "Bloquear Horário" do protótipo. */
    @Transactional
    public BloqueioDto bloquear(Long usuarioId, BloqueioRequest r) {
        Profissional p = profissionalDoUsuario(usuarioId);
        profissionais.findByIdForUpdate(p.getId());
        if (!r.horaFim().isAfter(r.horaInicio())) throw ApiException.invalido("O horário final deve ser depois do inicial");
        if (r.data().isBefore(LocalDate.now(clock))) throw ApiException.invalido("Não é possível bloquear uma data passada");
        boolean temAtendimento = agendamentos
                .findByProfissionalIdAndDataAndStatusNot(p.getId(), r.data(), StatusAgendamento.CANCELADO).stream()
                .anyMatch(a -> r.horaInicio().isBefore(a.getHoraFim()) && r.horaFim().isAfter(a.getHoraInicio()));
        if (temAtendimento) throw ApiException.conflito("Já existe atendimento neste período. Cancele-o antes de bloquear.");
        BloqueioHorario b = new BloqueioHorario();
        b.setProfissional(p);
        b.setData(r.data());
        b.setHoraInicio(r.horaInicio());
        b.setHoraFim(r.horaFim());
        b.setMotivo(r.motivo() == null || r.motivo().isBlank() ? null : r.motivo().trim());
        return Mapper.bloqueio(bloqueios.save(b));
    }

    @Transactional
    public void removerBloqueio(Long usuarioId, Long id) {
        BloqueioHorario b = bloqueios.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Bloqueio não encontrado"));
        if (!b.getProfissional().getUsuario().getId().equals(usuarioId)) throw ApiException.proibido("Bloqueio de outro profissional");
        bloqueios.delete(b);
    }

    @Transactional(readOnly = true)
    public List<ClienteResumoDto> clientesAtendidos(Long usuarioId) {
        Profissional p = profissionalDoUsuario(usuarioId);
        Map<Long, List<Agendamento>> porCliente = agendamentos.findByProfissionalId(p.getId()).stream()
                .filter(a -> a.getStatus() != StatusAgendamento.CANCELADO)
                .collect(Collectors.groupingBy(a -> a.getCliente().getId()));
        return porCliente.values().stream().map(lista -> {
            Usuario u = lista.get(0).getCliente().getUsuario();
            LocalDate ultimo = lista.stream().filter(a -> a.getStatus() == StatusAgendamento.REALIZADO)
                    .map(Agendamento::getData).max(Comparator.naturalOrder()).orElse(null);
            return new ClienteResumoDto(lista.get(0).getCliente().getId(), u.getNome(), u.getTelefone(), u.getEmail(), lista.size(), ultimo);
        }).sorted(Comparator.comparing(ClienteResumoDto::nome, String.CASE_INSENSITIVE_ORDER)).toList();
    }

    // =============================================================== helpers

    private StatusAgendamento statusInicial() {
        return autoConfirmar ? StatusAgendamento.CONFIRMADO : StatusAgendamento.PENDENTE;
    }

    private Agendamento buscar(Long id) {
        return agendamentos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Agendamento não encontrado"));
    }

    private Profissional profissionalDoUsuario(Long usuarioId) {
        return profissionais.findByUsuarioId(usuarioId)
                .orElseThrow(() -> ApiException.proibido("Cadastro de profissional não encontrado"));
    }

    private void exigirDonoCliente(Agendamento a, Long usuarioId) {
        if (!a.getCliente().getUsuario().getId().equals(usuarioId)) {
            throw ApiException.proibido("Você só pode alterar os seus próprios agendamentos");
        }
    }

    private Agendamento buscarDoProfissional(Long usuarioId, Long id) {
        Agendamento a = buscar(id);
        if (!a.getProfissional().getUsuario().getId().equals(usuarioId)) {
            throw ApiException.proibido("Este atendimento não pertence à sua agenda");
        }
        return a;
    }

    private void exigirAberto(Agendamento a) {
        if (!ABERTOS.contains(a.getStatus())) {
            throw ApiException.conflito("Este agendamento já está " + a.getStatus().name().toLowerCase());
        }
    }
}
