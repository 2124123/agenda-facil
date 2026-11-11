package br.com.agendafacil.repository;

import br.com.agendafacil.model.Agendamento;
import br.com.agendafacil.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long> {
    List<Agendamento> findByProfissionalIdAndDataAndStatusNot(Long profissionalId, LocalDate data, StatusAgendamento status);
    List<Agendamento> findByClienteIdOrderByDataDescHoraInicioDesc(Long clienteId);
    List<Agendamento> findByProfissionalIdAndDataBetweenOrderByDataAscHoraInicioAsc(Long profissionalId, LocalDate ini, LocalDate fim);
    List<Agendamento> findByProfissionalId(Long profissionalId);
    List<Agendamento> findByDataBetweenAndStatusNot(LocalDate ini, LocalDate fim, StatusAgendamento status);
    List<Agendamento> findByDataBetweenOrderByDataAscHoraInicioAsc(LocalDate ini, LocalDate fim);
    List<Agendamento> findByDataAndStatusInAndLembreteEnviadoFalse(LocalDate data, Collection<StatusAgendamento> status);
}
