package br.com.agendafacil.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "servico")
@Getter @Setter @NoArgsConstructor
public class Servico {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "estabelecimento_id", nullable = false)
    private Estabelecimento estabelecimento;
    @Column(nullable = false, length = 120)
    private String nome;
    @Column(length = 400)
    private String descricao;
    @Column(nullable = false, length = 60)
    private String categoria;
    @Column(nullable = false)
    private int duracaoMinutos;
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal preco;
    @Column(nullable = false)
    private boolean ativo = true;
}
