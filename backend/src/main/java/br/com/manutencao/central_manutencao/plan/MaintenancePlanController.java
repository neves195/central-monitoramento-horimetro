package br.com.manutencao.central_manutencao.plan;

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

@RestController
@RequestMapping("/api")
public class MaintenancePlanController {

    private final MaintenancePlanService service;

    public MaintenancePlanController(MaintenancePlanService service) {
        this.service = service;
    }

    // POST /api/equipments/{equipmentId}/plans -> 201 com o Location do novo plano
    @PostMapping("/equipments/{equipmentId}/plans")
    public ResponseEntity<MaintenancePlanResponse> create(@PathVariable Long equipmentId,
            @Valid @RequestBody MaintenancePlanRequest request) {
        MaintenancePlan created = service.create(equipmentId, toEntity(request));
        URI location = URI.create("/api/plans/" + created.getId());
        return ResponseEntity.created(location).body(MaintenancePlanResponse.from(created));
    }

    // GET /api/equipments/{equipmentId}/plans -> apenas os planos ativos
    @GetMapping("/equipments/{equipmentId}/plans")
    public List<MaintenancePlanResponse> listByEquipment(@PathVariable Long equipmentId) {
        return service.listByEquipment(equipmentId).stream().map(MaintenancePlanResponse::from).toList();
    }

    // GET /api/plans/{id}
    @GetMapping("/plans/{id}")
    public MaintenancePlanResponse findById(@PathVariable Long id) {
        return MaintenancePlanResponse.from(service.findById(id));
    }

    // PUT /api/plans/{id}
    @PutMapping("/plans/{id}")
    public MaintenancePlanResponse update(@PathVariable Long id, @Valid @RequestBody MaintenancePlanRequest request) {
        return MaintenancePlanResponse.from(service.update(id, toEntity(request)));
    }

    // DELETE /api/plans/{id} -> exclusão lógica (desativa), responde 204
    @DeleteMapping("/plans/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        service.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    // O equipamento vem do caminho da URL, não do corpo
    private MaintenancePlan toEntity(MaintenancePlanRequest request) {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setName(request.name());
        plan.setIntervalHours(request.intervalHours());
        plan.setWarningHours(request.warningHours());
        plan.setLastMaintenanceHours(request.lastMaintenanceHours());
        return plan;
    }
}
