package br.com.agendafacil.controller;

import br.com.agendafacil.dto.Dtos.*;
import br.com.agendafacil.service.AdminService;
import br.com.agendafacil.service.CatalogoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Todas as rotas /api/admin/** exigem perfil ADMINISTRADOR (RN005) - ver SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
    private final CatalogoService catalogo;
    private final AdminService admin;

    // ---- serviços
    @GetMapping("/servicos")
    public List<ServicoDto> servicos(@RequestParam(required = false) String busca) {
        return catalogo.servicosAdmin(busca);
    }

    @PostMapping("/servicos")
    @ResponseStatus(HttpStatus.CREATED)
    public ServicoDto criarServico(@Valid @RequestBody ServicoForm f) {
        return catalogo.criarServico(f);
    }

    @PutMapping("/servicos/{id}")
    public ServicoDto atualizarServico(@PathVariable Long id, @Valid @RequestBody ServicoForm f) {
        return catalogo.atualizarServico(id, f);
    }

    @PatchMapping("/servicos/{id}/ativo")
    public ServicoDto ativoServico(@PathVariable Long id, @RequestParam boolean valor) {
        return catalogo.alterarAtivoServico(id, valor);
    }

    // ---- profissionais
    @GetMapping("/profissionais")
    public List<ProfissionalDto> profissionais() {
        return catalogo.profissionaisAdmin();
    }

    @PostMapping("/profissionais")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfissionalDto criarProfissional(@Valid @RequestBody ProfissionalForm f) {
        return catalogo.criarProfissional(f);
    }

    @PutMapping("/profissionais/{id}")
    public ProfissionalDto atualizarProfissional(@PathVariable Long id, @Valid @RequestBody ProfissionalForm f) {
        return catalogo.atualizarProfissional(id, f);
    }

    @PatchMapping("/profissionais/{id}/ativo")
    public ProfissionalDto ativoProfissional(@PathVariable Long id, @RequestParam boolean valor) {
        return catalogo.alterarAtivoProfissional(id, valor);
    }

    @GetMapping("/profissionais/{id}/disponibilidade")
    public List<DisponibilidadeDto> disponibilidade(@PathVariable Long id) {
        return catalogo.disponibilidade(id);
    }

    @PutMapping("/profissionais/{id}/disponibilidade")
    public List<DisponibilidadeDto> definirDisponibilidade(@PathVariable Long id, @RequestBody List<DisponibilidadeDto> lista) {
        return catalogo.definirDisponibilidade(id, lista);
    }

    // ---- indicadores e relatórios
    @GetMapping("/dashboard")
    public DashboardDto dashboard() {
        return admin.dashboard();
    }

    @GetMapping("/relatorios/agendamentos")
    public RelatorioDto relatorio(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return admin.relatorio(inicio, fim);
    }
}
