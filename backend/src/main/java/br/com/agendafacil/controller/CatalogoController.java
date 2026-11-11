package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.service.CatalogoService;
import br.com.agendafacil.service.DisponibilidadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** Consultas disponíveis para qualquer usuário autenticado (RF003 e apoio ao agendamento). */
@RestController
@RequiredArgsConstructor
public class CatalogoController {
    private final CatalogoService catalogo;
    private final DisponibilidadeService disponibilidade;

    @GetMapping("/api/servicos")
    public List<ServicoDto> servicos(@RequestParam(required = false) String busca,
                                     @RequestParam(required = false) String categoria) {
        return catalogo.servicosAtivos(busca, categoria);
    }

    @GetMapping("/api/profissionais")
    public List<ProfissionalDto> profissionais(@RequestParam(required = false) Long servicoId) {
        return catalogo.profissionaisAtivos(servicoId);
    }

    @GetMapping("/api/agenda/dias")
    public List<DiaDisponivelDto> dias(@RequestParam Long profissionalId, @RequestParam Long servicoId,
                                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                       @RequestParam(defaultValue = "14") int dias) {
        return disponibilidade.dias(profissionalId, servicoId, inicio, dias);
    }

    @GetMapping("/api/agenda/horarios")
    public List<String> horarios(@RequestParam Long profissionalId, @RequestParam Long servicoId,
                                 @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
                                 @RequestParam(required = false) Long ignorarId) {
        return disponibilidade.horariosLivres(profissionalId, servicoId, data, ignorarId);
    }
}
