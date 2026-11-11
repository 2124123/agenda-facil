package br.com.agendafacil.dto;

import br.com.agendafacil.model.Perfil;
import br.com.agendafacil.model.StatusAgendamento;
import br.com.agendafacil.model.TipoNotificacao;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Todos os objetos de entrada/saída da API. Horários trafegam como "HH:mm". */
public final class Dtos {
    private Dtos() {}

    // ---------- autenticação / perfil ----------
    public record LoginRequest(@NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
                               @NotBlank(message = "Informe a senha") String senha,
                               Perfil perfil) {}

    public record RegisterRequest(@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
                                  @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
                                  @Size(max = 20) String telefone,
                                  @NotBlank(message = "Informe a senha") @Size(min = 6, max = 60, message = "A senha deve ter no mínimo 6 caracteres") String senha) {}

    public record UsuarioDto(Long id, String nome, String email, String telefone, Perfil perfil) {}

    public record AuthResponse(String token, UsuarioDto usuario, String estabelecimento) {}

    public record PerfilUpdate(@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
                               @Size(max = 20) String telefone) {}

    // ---------- serviços ----------
    public record ServicoDto(Long id, String nome, String descricao, String categoria,
                             int duracaoMinutos, BigDecimal preco, boolean ativo) {}

    public record ServicoForm(@NotBlank(message = "Informe o nome do serviço") @Size(max = 120) String nome,
                              @Size(max = 400) String descricao,
                              @NotBlank(message = "Informe a categoria") String categoria,
                              @Min(value = 5, message = "Duração mínima de 5 minutos") @Max(720) int duracaoMinutos,
                              @NotNull(message = "Informe o preço") @DecimalMin(value = "0.0", message = "Preço inválido") BigDecimal preco) {}

    // ---------- profissionais ----------
    public record ProfissionalDto(Long id, String nome, String email, String telefone, String especialidade, boolean ativo) {}

    public record ProfissionalForm(@NotBlank(message = "Informe o nome") @Size(max = 120) String nome,
                                   @NotBlank(message = "Informe o e-mail") @Email(message = "E-mail inválido") String email,
                                   @Size(max = 20) String telefone,
                                   @NotBlank(message = "Informe a especialidade") String especialidade,
                                   @Size(min = 6, max = 60, message = "A senha deve ter no mínimo 6 caracteres") String senha) {}

    public record DisponibilidadeDto(DayOfWeek diaSemana,
                                     @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                                     @JsonFormat(pattern = "HH:mm") LocalTime horaFim) {}

    // ---------- agendamentos ----------
    public record AgendamentoRequest(@NotNull Long profissionalId,
                                     @NotNull Long servicoId,
                                     @NotNull LocalDate data,
                                     @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaInicio) {}

    public record RemarcarRequest(@NotNull LocalDate data,
                                  @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaInicio) {}

    public record AgendamentoDto(Long id, Long servicoId, String servico, String categoria,
                                 Long profissionalId, String profissional,
                                 Long clienteId, String cliente, String clienteTelefone,
                                 LocalDate data,
                                 @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                                 @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
                                 StatusAgendamento status, BigDecimal valor,
                                 String local, String endereco) {}

    public record DiaDisponivelDto(LocalDate data, int horarios) {}

    // ---------- agenda do profissional ----------
    public record BloqueioRequest(@NotNull LocalDate data,
                                  @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                                  @NotNull @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
                                  @Size(max = 120) String motivo) {}

    public record BloqueioDto(Long id, LocalDate data,
                              @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
                              @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
                              String motivo) {}

    public record AgendaDto(List<AgendamentoDto> agendamentos, List<BloqueioDto> bloqueios) {}

    public record ClienteResumoDto(Long id, String nome, String telefone, String email,
                                   long atendimentos, LocalDate ultimoAtendimento) {}

    // ---------- notificações ----------
    public record NotificacaoDto(Long id, TipoNotificacao tipo, String mensagem, boolean lida, LocalDateTime criadaEm) {}

    // ---------- administrador ----------
    public record ProfissionalResumoDto(Long id, String nome, String especialidade, long agendamentosHoje) {}

    public record DashboardDto(String estabelecimento, long agendamentosHoje, Integer variacaoPercentual,
                               int taxaOcupacao, List<Integer> semana, List<ProfissionalResumoDto> profissionais) {}

    public record RelatorioDto(LocalDate inicio, LocalDate fim, long total, long pendentes, long confirmados,
                               long realizados, long cancelados, BigDecimal receita,
                               List<AgendamentoDto> itens) {}
}
