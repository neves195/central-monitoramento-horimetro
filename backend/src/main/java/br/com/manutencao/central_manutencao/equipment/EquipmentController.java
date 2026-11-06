package br.com.manutencao.central_manutencao.equipment;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * CRUD de equipamentos. A exclusão é lógica (desativa) para preservar o histórico.
 * Leituras (GET) aceitam ADMIN e VIEWER; escritas (POST, PUT, DELETE) exigem ADMIN.
 */
@RestController
@RequestMapping("/api/equipments")
public class EquipmentController {

    private final EquipmentService service;

    /** Injeta as dependências. */
    public EquipmentController(EquipmentService service) {
        this.service = service;
    }

    /** POST /api/equipments (ADMIN). Cria o equipamento e responde 201 com o Location dele. */
    @PostMapping
    public ResponseEntity<EquipmentResponse> create(@Valid @RequestBody EquipmentRequest request) {
        Equipment created = service.create(toEntity(request));
        URI location = URI.create("/api/equipments/" + created.getId());
        return ResponseEntity.created(location).body(EquipmentResponse.from(created));
    }

    /** GET /api/equipments (ADMIN ou VIEWER). Lista apenas os equipamentos ativos. */
    @GetMapping
    public List<EquipmentResponse> list() {
        return service.list().stream().map(EquipmentResponse::from).toList();
    }

    /** GET /api/equipments/{id} (ADMIN ou VIEWER). Busca por id, inclusive equipamento desativado. */
    @GetMapping("/{id}")
    public EquipmentResponse findById(@PathVariable Long id) {
        return EquipmentResponse.from(service.findById(id));
    }

    /** PUT /api/equipments/{id} (ADMIN). Atualiza código, nome e categoria; o horímetro só muda por leitura. */
    @PutMapping("/{id}")
    public EquipmentResponse update(@PathVariable Long id, @Valid @RequestBody EquipmentRequest request) {
        return EquipmentResponse.from(service.update(id, toEntity(request)));
    }

    /** DELETE /api/equipments/{id} (ADMIN). Exclusão lógica: desativa e responde 204. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    /** POST /api/equipments/{id}/reactivate (ADMIN). Reativa um equipamento desativado. */
    @PostMapping("/{id}/reactivate")
    public EquipmentResponse reactivate(@PathVariable Long id) {
        return EquipmentResponse.from(service.reactivate(id));
    }

    // O request não carrega horímetro nem status: o serviço define os padrões
    private Equipment toEntity(EquipmentRequest request) {
        Equipment equipment = new Equipment();
        equipment.setCode(request.code());
        equipment.setName(request.name());
        equipment.setCategory(request.category());
        return equipment;
    }
}
