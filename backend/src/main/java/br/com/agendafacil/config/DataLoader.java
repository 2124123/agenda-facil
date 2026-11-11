package br.com.agendafacil.config;

import br.com.agendafacil.model.*;
import br.com.agendafacil.repository.*;
import br.com.agendafacil.service.CatalogoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;

/** Popula o banco com os dados do protótipo do Figma na primeira execução (agenda.seed=true). */
@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {
    private static final String SENHA = "12345678";

    private final UsuarioRepository usuarios;
    private final ClienteRepository clientes;
    private final ProfissionalRepository profissionais;
    private final ServicoRepository servicos;
    private final EstabelecimentoRepository estabelecimentos;
    private final AgendamentoRepository agendamentos;
    private final CatalogoService catalogo;
    private final PasswordEncoder encoder;
    private final Clock clock;

    @Value("${agenda.seed:true}")
    private boolean seed;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seed || usuarios.count() > 0) return;

        Estabelecimento est = new Estabelecimento();
        est.setNome("Studio Beauty");
        est.setEndereco("Rua Augusta, 1200 - São Paulo");
        est.setTelefone("(11) 4002-8922");
        est = estabelecimentos.save(est);

        usuario("Administrador Studio", "admin@agendafacil.com", "(11) 90000-0000", Perfil.ADMINISTRADOR);

        Profissional ana = profissional(est, "Ana Paula", "ana@agendafacil.com", "Cabelo");
        Profissional mariana = profissional(est, "Mariana Costa", "mariana@agendafacil.com", "Estética");
        Profissional carlos = profissional(est, "Carlos Souza", "carlos@agendafacil.com", "Unhas");
        profissional(est, "Juliana Prado", "juliana@agendafacil.com", "Consultoria");

        Cliente beatriz = cliente("Beatriz Xavier", "beatriz@email.com");
        Cliente caue = cliente("Cauê Sena", "caue@email.com");
        Cliente stefany = cliente("Stefany Ferreira", "stefany@email.com");
        Cliente gabriel = cliente("Gabriel Santos", "gabriel@email.com");

        Servico corteEscova = servico(est, "Corte e Escova", "Cabelo", 60, "85.00", "Corte personalizado com escova modeladora.");
        Servico corte = servico(est, "Corte de Cabelo", "Cabelo", 45, "60.00", "Corte feminino ou masculino.");
        Servico progressiva = servico(est, "Escova Progressiva", "Cabelo", 120, "220.00", "Alisamento progressivo.");
        Servico coloracao = servico(est, "Coloração", "Cabelo", 90, "150.00", "Coloração completa.");
        Servico facial = servico(est, "Estética Facial", "Estética", 60, "120.00", "Tratamento facial completo.");
        Servico limpeza = servico(est, "Limpeza de Pele", "Estética", 75, "140.00", "Limpeza profunda com extração.");
        Servico manicure = servico(est, "Manicure", "Unhas", 45, "40.00", "Cuidado completo das unhas das mãos.");
        servico(est, "Pedicure", "Unhas", 60, "50.00", "Cuidado completo das unhas dos pés.");
        servico(est, "Consultoria de Imagem", "Consultoria", 60, "180.00", "Análise de estilo e imagem pessoal.");

        LocalDate hoje = LocalDate.now(clock);
        ag(caue, ana, progressiva, hoje, 10, 30, StatusAgendamento.CONFIRMADO);
        ag(beatriz, ana, corte, hoje, 9, 0, StatusAgendamento.CONFIRMADO);
        ag(stefany, ana, corteEscova, hoje, 14, 0, StatusAgendamento.PENDENTE);
        ag(gabriel, ana, coloracao, hoje, 15, 30, StatusAgendamento.CONFIRMADO);
        ag(beatriz, mariana, limpeza, hoje, 11, 0, StatusAgendamento.CONFIRMADO);
        ag(stefany, carlos, manicure, hoje, 10, 0, StatusAgendamento.CONFIRMADO);
        ag(beatriz, ana, corteEscova, hoje.plusDays(1), 14, 0, StatusAgendamento.CONFIRMADO);
        ag(beatriz, mariana, facial, hoje.plusDays(3), 9, 30, StatusAgendamento.PENDENTE);
        ag(beatriz, carlos, manicure, hoje.minusDays(1), 16, 0, StatusAgendamento.REALIZADO);
        ag(gabriel, ana, corte, hoje.minusDays(1), 10, 0, StatusAgendamento.REALIZADO);
    }

    private Usuario usuario(String nome, String email, String tel, Perfil perfil) {
        Usuario u = new Usuario();
        u.setNome(nome);
        u.setEmail(email);
        u.setTelefone(tel);
        u.setSenha(encoder.encode(SENHA));
        u.setPerfil(perfil);
        return usuarios.save(u);
    }

    private Cliente cliente(String nome, String email) {
        Cliente c = new Cliente();
        c.setUsuario(usuario(nome, email, "(11) 9" + Math.abs(email.hashCode() % 10000) + "-1234", Perfil.CLIENTE));
        return clientes.save(c);
    }

    private Profissional profissional(Estabelecimento est, String nome, String email, String especialidade) {
        Profissional p = new Profissional();
        p.setEstabelecimento(est);
        p.setUsuario(usuario(nome, email, "(11) 98888-0000", Perfil.PROFISSIONAL));
        p.setEspecialidade(especialidade);
        p = profissionais.save(p);
        catalogo.expedientePadrao(p);
        return p;
    }

    private Servico servico(Estabelecimento est, String nome, String cat, int min, String preco, String desc) {
        Servico s = new Servico();
        s.setEstabelecimento(est);
        s.setNome(nome);
        s.setCategoria(cat);
        s.setDuracaoMinutos(min);
        s.setPreco(new BigDecimal(preco));
        s.setDescricao(desc);
        return servicos.save(s);
    }

    private void ag(Cliente c, Profissional p, Servico s, LocalDate dia, int h, int m, StatusAgendamento st) {
        Agendamento a = new Agendamento();
        a.setCliente(c);
        a.setProfissional(p);
        a.setServico(s);
        a.setData(dia);
        a.setHoraInicio(LocalTime.of(h, m));
        a.setHoraFim(LocalTime.of(h, m).plusMinutes(s.getDuracaoMinutos()));
        a.setValor(s.getPreco());
        a.setStatus(st);
        agendamentos.save(a);
    }
}
