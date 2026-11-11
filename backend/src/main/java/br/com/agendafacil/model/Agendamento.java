package br.com.agendafacil.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "agendamento", indexes = @Index(name = "idx_ag_prof_data", columnList = "profissional_id, data"))
@Getter @Setter @NoArgsConstructor
public class Agendamento {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Cliente cliente;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "profissional_id", nullable = false)
    private Profissional profissional;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "servico_id", nullable = false)
    private Servico servico;
    @Column(nullable = false)
    private LocalDate data;
    @Column(nullable = false)
    private LocalTime horaInicio;
    @Column(nullable = false)
    private LocalTime horaFim;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 12)
    private StatusAgendamento status = StatusAgendamento.PENDENTE;
    /** Preço do serviço no momento do agendamento. */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;
    @Column(nullable = false)
    private boolean lembreteEnviado = false;
    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
