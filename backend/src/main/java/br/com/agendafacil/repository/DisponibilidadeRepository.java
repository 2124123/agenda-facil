package br.com.agendafacil.repository;

import br.com.agendafacil.model.Disponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.DayOfWeek;
import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {
    List<Disponibilidade> findByProfissionalIdAndDiaSemana(Long profissionalId, DayOfWeek diaSemana);
    List<Disponibilidade> findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(Long profissionalId);
    List<Disponibilidade> findByDiaSemana(DayOfWeek diaSemana);

    @Modifying
    @Query("delete from Disponibilidade d where d.profissional.id = :pid")
    void apagarDoProfissional(@Param("pid") Long profissionalId);
}
