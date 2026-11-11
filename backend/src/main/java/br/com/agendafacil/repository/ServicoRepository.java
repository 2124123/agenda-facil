package br.com.agendafacil.repository;

import br.com.agendafacil.model.Servico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ServicoRepository extends JpaRepository<Servico, Long> {

    @Query("""
           select s from Servico s
           where (:apenasAtivos = false or s.ativo = true)
             and (:categoria = '' or lower(s.categoria) = lower(:categoria))
             and (lower(s.nome) like lower(concat('%', :busca, '%'))
                  or lower(s.categoria) like lower(concat('%', :busca, '%'))
                  or lower(coalesce(s.descricao, '')) like lower(concat('%', :busca, '%')))
           order by s.nome
           """)
    List<Servico> buscar(@Param("busca") String busca,
                         @Param("categoria") String categoria,
                         @Param("apenasAtivos") boolean apenasAtivos);

    @Query("select s from Servico s where s.ativo = true and lower(s.categoria) = lower(:categoria) order by s.nome")
    List<Servico> ativosDaCategoria(@Param("categoria") String categoria);
}
