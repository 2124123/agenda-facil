package br.com.agendafacil.service;

import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.TipoNotificacao;
import br.com.agendafacil.repository.AgendamentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Lembretes automáticos 30 minutos antes do atendimento (benefício 03 do documento de visão). */
@Component
@RequiredArgsConstructor
public class LembreteScheduler {
    private final AgendamentoRepository agendamentos;
    private final NotificacaoService notificacoes;
    private final Clock clock;

    @Scheduled(fixedDelay = 60_000, initialDelay = 30_000)
    @Transactional
    public void enviarLembretes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Agendamento> candidatos = agendamentos.findByDataAndStatusInAndLembreteEnviadoFalse(
                agora.toLocalDate(), List.of(StatusAgendamento.PENDENTE, StatusAgendamento.CONFIRMADO));
        for (Agendamento a : candidatos) {
            long min = Duration.between(agora, a.getData().atTime(a.getHoraInicio())).toMinutes();
            if (min < 0 || min > 30) continue;
            String hora = a.getHoraInicio().format(DateTimeFormatter.ofPattern("HH:mm"));
            notificacoes.notificar(a.getCliente().getUsuario(), TipoNotificacao.LEMBRETE,
                    "Lembrete: " + a.getServico().getNome() + " com " + a.getProfissional().getUsuario().getNome()
                            + " às " + hora + " (em " + min + " minutos)");
            notificacoes.notificar(a.getProfissional().getUsuario(), TipoNotificacao.LEMBRETE,
                    "Lembrete: atendimento com " + a.getCliente().getUsuario().getNome() + " às " + hora + " (em " + min + " minutos)");
            a.setLembreteEnviado(true);
        }
    }
}
