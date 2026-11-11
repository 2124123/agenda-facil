package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {
    private final AgendamentoRepository agendamentos;
    private final ProfissionalRepository profissionais;
    private final DisponibilidadeRepository disponibilidades;
    private final EstabelecimentoRepository estabelecimentos;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardDto dashboard() {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ontem = hoje.minusDays(1);
        List<Agendamento> doDia = agendamentos.findByDataBetweenAndStatusNot(hoje, hoje, StatusAgendamento.CANCELADO);
        long qtdHoje = doDia.size();
        long qtdOntem = agendamentos.findByDataBetweenAndStatusNot(ontem, ontem, StatusAgendamento.CANCELADO).size();
        Integer variacao = qtdOntem == 0 ? null : (int) Math.round((qtdHoje - qtdOntem) * 100.0 / qtdOntem);

        List<Profissional> ativos = profissionais.findByUsuarioStatusOrderByUsuarioNomeAsc(StatusUsuario.ATIVO);
        Set<Long> idsAtivos = ativos.stream().map(Profissional::getId).collect(Collectors.toSet());

        long minutosDisponiveis = disponibilidades.findByDiaSemana(hoje.getDayOfWeek()).stream()
                .filter(d -> idsAtivos.contains(d.getProfissional().getId()))
                .mapToLong(d -> ChronoUnit.MINUTES.between(d.getHoraInicio(), d.getHoraFim())).sum();
        long minutosOcupados = doDia.stream()
                .filter(a -> idsAtivos.contains(a.getProfissional().getId()))
                .mapToLong(a -> ChronoUnit.MINUTES.between(a.getHoraInicio(), a.getHoraFim())).sum();
        int taxa = minutosDisponiveis == 0 ? 0 : (int) Math.min(100, Math.round(minutosOcupados * 100.0 / minutosDisponiveis));

        LocalDate segunda = hoje.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        Map<LocalDate, Long> porDia = agendamentos
                .findByDataBetweenAndStatusNot(segunda, segunda.plusDays(6), StatusAgendamento.CANCELADO).stream()
                .collect(Collectors.groupingBy(Agendamento::getData, Collectors.counting()));
        List<Integer> semana = new ArrayList<>();
        for (int i = 0; i < 7; i++) semana.add(porDia.getOrDefault(segunda.plusDays(i), 0L).intValue());

        Map<Long, Long> porProf = doDia.stream().collect(Collectors.groupingBy(a -> a.getProfissional().getId(), Collectors.counting()));
        List<ProfissionalResumoDto> resumo = ativos.stream()
                .map(p -> new ProfissionalResumoDto(p.getId(), p.getUsuario().getNome(), p.getEspecialidade(), porProf.getOrDefault(p.getId(), 0L)))
                .toList();

        String nome = estabelecimentos.findAll().stream().findFirst().map(Estabelecimento::getNome).orElse("Agenda Fácil");
        return new DashboardDto(nome, qtdHoje, variacao, taxa, semana, resumo);
    }

    /** RF002: relatório de agendamentos por período. */
    @Transactional(readOnly = true)
    public RelatorioDto relatorio(LocalDate inicio, LocalDate fim) {
        LocalDate hoje = LocalDate.now(clock);
        LocalDate ini = inicio != null ? inicio : hoje.withDayOfMonth(1);
        LocalDate f = fim != null ? fim : hoje;
        if (f.isBefore(ini)) throw ApiException.invalido("A data final deve ser posterior à inicial");
        if (ChronoUnit.DAYS.between(ini, f) > 366) throw ApiException.invalido("Período máximo de 366 dias");
        List<Agendamento> lista = agendamentos.findByDataBetweenOrderByDataAscHoraInicioAsc(ini, f);
        Map<StatusAgendamento, Long> cont = lista.stream().collect(Collectors.groupingBy(Agendamento::getStatus, Collectors.counting()));
        BigDecimal receita = lista.stream().filter(a -> a.getStatus() == StatusAgendamento.REALIZADO)
                .map(Agendamento::getValor).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RelatorioDto(ini, f, lista.size(),
                cont.getOrDefault(StatusAgendamento.PENDENTE, 0L), cont.getOrDefault(StatusAgendamento.CONFIRMADO, 0L),
                cont.getOrDefault(StatusAgendamento.REALIZADO, 0L), cont.getOrDefault(StatusAgendamento.CANCELADO, 0L),
                receita, lista.stream().map(Mapper::agendamento).toList());
    }
}
