package br.com.agendafacil.repository;

import br.com.agendafacil.model.BloqueioHorario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface BloqueioHorarioRepository extends JpaRepository<BloqueioHorario, Long> {
    List<BloqueioHorario> findByProfissionalIdAndData(Long profissionalId, LocalDate data);
    List<BloqueioHorario> findByProfissionalIdAndDataBetweenOrderByDataAscHoraInicioAsc(Long profissionalId, LocalDate ini, LocalDate fim);
}
