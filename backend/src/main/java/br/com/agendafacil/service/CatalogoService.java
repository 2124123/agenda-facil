package br.com.agendafacil.service;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.exception.ApiException;
import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.*;

/** Serviços e profissionais: consulta (cliente) e manutenção (somente administrador - RN005). */
@Service
@RequiredArgsConstructor
public class CatalogoService {
    private final ServicoRepository servicos;
    private final ProfissionalRepository profissionais;
    private final UsuarioRepository usuarios;
    private final DisponibilidadeRepository disponibilidades;
    private final EstabelecimentoRepository estabelecimentos;
    private final PasswordEncoder encoder;

    // ------------------------------------------------------------ consulta (RF003)

    @Transactional(readOnly = true)
    public List<ServicoDto> servicosAtivos(String busca, String categoria) {
        String q = busca == null ? "" : busca.trim();
        String cat = categoria == null ? "" : categoria.trim();
        Map<Long, Servico> res = new LinkedHashMap<>();
        servicos.buscar(q, cat, true).forEach(s -> res.put(s.getId(), s));
        if (!q.isEmpty()) {
            // a busca também encontra serviços pelo nome do profissional que os realiza
            for (Profissional p : profissionais.findByUsuarioStatusOrderByUsuarioNomeAsc(StatusUsuario.ATIVO)) {
                if (p.getUsuario().getNome().toLowerCase().contains(q.toLowerCase())) {
                    servicos.ativosDaCategoria(p.getEspecialidade()).stream()
                            .filter(s -> cat.isEmpty() || s.getCategoria().equalsIgnoreCase(cat))
                            .forEach(s -> res.putIfAbsent(s.getId(), s));
                }
            }
        }
        return res.values().stream().sorted(Comparator.comparing(Servico::getNome)).map(Mapper::servico).toList();
    }

    @Transactional(readOnly = true)
    public List<ProfissionalDto> profissionaisAtivos(Long servicoId) {
        List<Profissional> lista;
        if (servicoId == null) {
            lista = profissionais.findByUsuarioStatusOrderByUsuarioNomeAsc(StatusUsuario.ATIVO);
        } else {
            Servico s = servicos.findById(servicoId).orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado"));
            lista = profissionais.findByUsuarioStatusAndEspecialidadeIgnoreCaseOrderByUsuarioNomeAsc(StatusUsuario.ATIVO, s.getCategoria());
        }
        return lista.stream().map(Mapper::profissional).toList();
    }

    // ------------------------------------------------------------ administração de serviços

    @Transactional(readOnly = true)
    public List<ServicoDto> servicosAdmin(String busca) {
        return servicos.buscar(busca == null ? "" : busca.trim(), "", false).stream().map(Mapper::servico).toList();
    }

    @Transactional
    public ServicoDto criarServico(ServicoForm f) {
        Servico s = new Servico();
        s.setEstabelecimento(estabelecimento());
        aplicar(s, f);
        return Mapper.servico(servicos.save(s));
    }

    @Transactional
    public ServicoDto atualizarServico(Long id, ServicoForm f) {
        Servico s = servicos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado"));
        aplicar(s, f);
        return Mapper.servico(s);
    }

    @Transactional
    public ServicoDto alterarAtivoServico(Long id, boolean ativo) {
        Servico s = servicos.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Serviço não encontrado"));
        s.setAtivo(ativo);
        return Mapper.servico(s);
    }

    private void aplicar(Servico s, ServicoForm f) {
        s.setNome(f.nome().trim());
        s.setDescricao(f.descricao() == null || f.descricao().isBlank() ? null : f.descricao().trim());
        s.setCategoria(f.categoria().trim());
        s.setDuracaoMinutos(f.duracaoMinutos());
        s.setPreco(f.preco());
    }

    // ------------------------------------------------------------ administração de profissionais

    @Transactional(readOnly = true)
    public List<ProfissionalDto> profissionaisAdmin() {
        return profissionais.findAllByOrderByUsuarioNomeAsc().stream().map(Mapper::profissional).toList();
    }

    @Transactional
    public ProfissionalDto criarProfissional(ProfissionalForm f) {
        if (f.senha() == null || f.senha().isBlank()) throw ApiException.invalido("Informe uma senha inicial para o profissional");
        String email = f.email().trim().toLowerCase();
        if (usuarios.existsByEmailIgnoreCase(email)) throw ApiException.conflito("E-mail já cadastrado");
        Usuario u = new Usuario();
        u.setNome(f.nome().trim());
        u.setEmail(email);
        u.setTelefone(f.telefone() == null || f.telefone().isBlank() ? null : f.telefone().trim());
        u.setSenha(encoder.encode(f.senha()));
        u.setPerfil(Perfil.PROFISSIONAL);
        u = usuarios.save(u);
        Profissional p = new Profissional();
        p.setEstabelecimento(estabelecimento());
        p.setUsuario(u);
        p.setEspecialidade(f.especialidade().trim());
        p = profissionais.save(p);
        expedientePadrao(p);
        return Mapper.profissional(p);
    }

    @Transactional
    public ProfissionalDto atualizarProfissional(Long id, ProfissionalForm f) {
        Profissional p = profissional(id);
        Usuario u = p.getUsuario();
        u.setNome(f.nome().trim());
        u.setTelefone(f.telefone() == null || f.telefone().isBlank() ? null : f.telefone().trim());
        p.setEspecialidade(f.especialidade().trim());
        if (f.senha() != null && !f.senha().isBlank()) u.setSenha(encoder.encode(f.senha()));
        return Mapper.profissional(p);
    }

    @Transactional
    public ProfissionalDto alterarAtivoProfissional(Long id, boolean ativo) {
        Profissional p = profissional(id);
        p.getUsuario().setStatus(ativo ? StatusUsuario.ATIVO : StatusUsuario.INATIVO);
        return Mapper.profissional(p);
    }

    @Transactional(readOnly = true)
    public List<DisponibilidadeDto> disponibilidade(Long profissionalId) {
        profissional(profissionalId);
        return disponibilidades.findByProfissionalIdOrderByDiaSemanaAscHoraInicioAsc(profissionalId).stream()
                .map(d -> new DisponibilidadeDto(d.getDiaSemana(), d.getHoraInicio(), d.getHoraFim())).toList();
    }

    @Transactional
    public List<DisponibilidadeDto> definirDisponibilidade(Long profissionalId, List<DisponibilidadeDto> novas) {
        Profissional p = profissional(profissionalId);
        for (DisponibilidadeDto d : novas) {
            if (d.diaSemana() == null || d.horaInicio() == null || d.horaFim() == null || !d.horaFim().isAfter(d.horaInicio())) {
                throw ApiException.invalido("Horário final deve ser depois do inicial");
            }
        }
        disponibilidades.apagarDoProfissional(profissionalId);
        disponibilidades.flush();
        for (DisponibilidadeDto d : novas) {
            Disponibilidade nova = new Disponibilidade();
            nova.setProfissional(p);
            nova.setDiaSemana(d.diaSemana());
            nova.setHoraInicio(d.horaInicio());
            nova.setHoraFim(d.horaFim());
            disponibilidades.save(nova);
        }
        return novas;
    }

    /** Segunda a sábado, 09:00 - 18:00. */
    public void expedientePadrao(Profissional p) {
        for (DayOfWeek dia : DayOfWeek.values()) {
            if (dia == DayOfWeek.SUNDAY) continue;
            Disponibilidade d = new Disponibilidade();
            d.setProfissional(p);
            d.setDiaSemana(dia);
            d.setHoraInicio(LocalTime.of(9, 0));
            d.setHoraFim(LocalTime.of(18, 0));
            disponibilidades.save(d);
        }
    }

    private Profissional profissional(Long id) {
        return profissionais.findById(id).orElseThrow(() -> ApiException.naoEncontrado("Profissional não encontrado"));
    }

    private Estabelecimento estabelecimento() {
        return estabelecimentos.findAll().stream().findFirst()
                .orElseThrow(() -> ApiException.naoEncontrado("Estabelecimento não configurado"));
    }
}
