package br.com.agendafacil.repository;

import br.com.agendafacil.model.Profissional;
import br.com.agendafacil.model.StatusUsuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfissionalRepository extends JpaRepository<Profissional, Long> {
    Optional<Profissional> findByUsuarioId(Long usuarioId);
    List<Profissional> findByUsuarioStatusOrderByUsuarioNomeAsc(StatusUsuario status);
    List<Profissional> findByUsuarioStatusAndEspecialidadeIgnoreCaseOrderByUsuarioNomeAsc(StatusUsuario status, String especialidade);
    List<Profissional> findAllByOrderByUsuarioNomeAsc();

    /** Trava o profissional durante a transação para impedir dois agendamentos simultâneos no mesmo horário (RN002). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Profissional p where p.id = :id")
    Optional<Profissional> findByIdForUpdate(@Param("id") Long id);
}
