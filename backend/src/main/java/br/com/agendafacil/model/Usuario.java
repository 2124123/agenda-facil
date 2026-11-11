package br.com.agendafacil.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuario")
@Getter @Setter @NoArgsConstructor
public class Usuario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(nullable = false, unique = true, length = 160)
    private String email;
    /** Hash BCrypt - nunca a senha em texto puro. */
    @Column(nullable = false)
    private String senha;
    @Column(length = 20)
    private String telefone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private Perfil perfil;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 10)
    private StatusUsuario status = StatusUsuario.ATIVO;
    @Column(nullable = false)
    private LocalDateTime dataCadastro = LocalDateTime.now();
}
