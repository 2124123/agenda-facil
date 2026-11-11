package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.DiaDisponivelDto;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/** Cálculo de horários livres e validação de conflitos (RN002). */
@Service
@RequiredArgsConstructor
public class DisponibilidadeService {
    private static final int PASSO_MINUTOS = 30;

    private final DisponibilidadeRepository disponibilidades;
    private final AgendamentoRepository agendamentos;
    private final BloqueioHorarioRepository bloqueios;
    private final ServicoRepository servicos;
    private final ProfissionalRepository profissionais;
    private final Clock clock;

    private record Contexto(List<Disponibilidade> janelas, List<Agendamento> ocupados,
                            List<BloqueioHorario> bloqueados, LocalDateTime agora) {}

    private Contexto contexto(Long profissionalId, LocalDate data, Long ignorarAgendamentoId) {
        List<Agendamento> ocupados = agendamentos
                .findByProfissionalIdAndDataAndStatusNot(profissionalId, data, StatusAgendamento.CANCELADO).stream()
                .filter(a -> !a.getId().equals(ignorarAgendamentoId))
                .toList();
        return new Contexto(
                disponibilidades.findByProfissionalIdAndDiaSemana(profissionalId, data.getDayOfWeek()),
                ocupados,
                bloqueios.findByProfissionalIdAndData(profissionalId, data),
                LocalDateTime.now(clock));
    }

    private static boolean sobrepoe(LocalTime ini1, LocalTime fim1, LocalTime ini2, LocalTime fim2) {
        return ini1.isBefore(fim2) && fim1.isAfter(ini2);
    }

    /** Retorna o motivo da indisponibilidade ou null quando o intervalo está livre. */
    private String motivo(Contexto c, LocalDate data, LocalTime ini, LocalTime fim) {
        if (!fim.isAfter(ini)) return "Horário inválido";
        if (!data.atTime(ini).isAfter(c.agora())) return "Não é possível agendar em um horário que já passou";
        boolean dentroDoExpediente = c.janelas().stream()
                .anyMatch(j -> !ini.isBefore(j.getHoraInicio()) && !fim.isAfter(j.getHoraFim()));
        if (!dentroDoExpediente) return "O profissional não atende neste dia ou horário";
        if (c.ocupados().stream().anyMatch(a -> sobrepoe(ini, fim, a.getHoraInicio(), a.getHoraFim()))) {
            return "Horário indisponível: o profissional já possui um atendimento neste período";
        }
        if (c.bloqueados().stream().anyMatch(b -> sobrepoe(ini, fim, b.getHoraInicio(), b.getHoraFim()))) {
            return "Horário indisponível: o profissional bloqueou este período";
        }
        return null;
    }

    /** Lança 409 se o intervalo não puder ser usado. Chamar com o profissional já travado. */
    public void validarOuFalhar(Long profissionalId, LocalDate data, LocalTime ini, LocalTime fim, Long ignorarAgendamentoId) {
        String m = motivo(contexto(profissionalId, data, ignorarAgendamentoId), data, ini, fim);
        if (m != null) throw ApiException.conflito(m);
    }

    @Transactional(readOnly = true)
    public List<String> horariosLivres(Long profissionalId, Long servicoId, LocalDate data, Long ignorarAgendamentoId) {
        Servico s = servicos.findById(servicoId).orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado"));
        Profissional p = profissionais.findById(profissionalId).orElseThrow(() -> ApiException.naoEncontrado("Profissional não encontrado"));
        if (!p.isAtivo()) return List.of();
        return calcular(p.getId(), s.getDuracaoMinutos(), data, ignorarAgendamentoId).stream().map(LocalTime::toString).toList();
    }

    @Transactional(readOnly = true)
    public List<DiaDisponivelDto> dias(Long profissionalId, Long servicoId, LocalDate inicio, int quantidade) {
        Servico s = servicos.findById(servicoId).orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado"));
        Profissional p = profissionais.findById(profissionalId).orElseThrow(() -> ApiException.naoEncontrado("Profissional não encontrado"));
        int n = Math.max(1, Math.min(quantidade, 60));
        List<DiaDisponivelDto> res = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            LocalDate d = inicio.plusDays(i);
            int qtd = p.isAtivo() ? calcular(p.getId(), s.getDuracaoMinutos(), d, null).size() : 0;
            res.add(new DiaDisponivelDto(d, qtd));
        }
        return res;
    }

    private TreeSet<LocalTime> calcular(Long profissionalId, int duracao, LocalDate data, Long ignorarId) {
        Contexto c = contexto(profissionalId, data, ignorarId);
        TreeSet<LocalTime> livres = new TreeSet<>();
        for (Disponibilidade j : c.janelas()) {
            LocalTime t = j.getHoraInicio();
            while (true) {
                LocalTime fim = t.plusMinutes(duracao);
                if (!fim.isAfter(t) || fim.isAfter(j.getHoraFim())) break;
                if (motivo(c, data, t, fim) == null) livres.add(t);
                LocalTime prox = t.plusMinutes(PASSO_MINUTOS);
                if (!prox.isAfter(t)) break;
                t = prox;
            }
        }
        return livres;
    }
}
