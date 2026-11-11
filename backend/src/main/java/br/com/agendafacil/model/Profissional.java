package br.com.agendafacil.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "profissional")
@Getter @Setter @NoArgsConstructor
public class Profissional {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;
    @OneToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
    /** Corresponde à categoria dos serviços que o profissional atende (Cabelo, Estética, Unhas, Consultoria). */
    @Column(nullable = false, length = 60)
    private String especialidade;

    public boolean isAtivo() {
        return usuario.getStatus() == StatusUsuario.ATIVO;
    }
}
