package br.com.manutencao.central_manutencao.maintenance;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
public class MaintenanceController {

    private final MaintenanceService service;

    public MaintenanceController(MaintenanceService service) {
        this.service = service;
    }

    // POST /api/equipments/{equipmentId}/maintenances -> 201, nasce como SCHEDULED
    @PostMapping("/equipments/{equipmentId}/maintenances")
    public ResponseEntity<MaintenanceResponse> schedule(@PathVariable Long equipmentId,
            @Valid @RequestBody MaintenanceRequest request) {
        MaintenanceRecord created = service.schedule(equipmentId, request.planId(), toEntity(request));
        URI location = URI.create("/api/maintenances/" + created.getId());
        return ResponseEntity.created(location).body(MaintenanceResponse.from(created));
    }

    // GET /api/equipments/{equipmentId}/maintenances?status=SCHEDULED (status é opcional)
    @GetMapping("/equipments/{equipmentId}/maintenances")
    public List<MaintenanceResponse> listByEquipment(@PathVariable Long equipmentId,
            @RequestParam(required = false) MaintenanceStatus status) {
        return service.listByEquipment(equipmentId, status).stream().map(MaintenanceResponse::from).toList();
    }

    // GET /api/maintenances/{id}
    @GetMapping("/maintenances/{id}")
    public MaintenanceResponse findById(@PathVariable Long id) {
        return MaintenanceResponse.from(service.findById(id));
    }

    // POST /api/maintenances/{id}/complete -> conclui e, se houver plano, zera a contagem dele
    // O corpo é opcional: sem corpo, vale hoje e o horímetro atual
    @PostMapping("/maintenances/{id}/complete")
    public MaintenanceResponse complete(@PathVariable Long id,
            @Valid @RequestBody(required = false) MaintenanceCompletionRequest request) {
        MaintenanceCompletionRequest body = request != null ? request : new MaintenanceCompletionRequest(null, null, null);
        return MaintenanceResponse.from(service.complete(id, body.performedAt(), body.hoursAtMaintenance(), body.responsible()));
    }

    // POST /api/maintenances/{id}/cancel
    @PostMapping("/maintenances/{id}/cancel")
    public MaintenanceResponse cancel(@PathVariable Long id) {
        return MaintenanceResponse.from(service.cancel(id));
    }

    // Equipamento e plano são resolvidos no service: aqui só os dados do corpo
    private MaintenanceRecord toEntity(MaintenanceRequest request) {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setScheduledDate(request.scheduledDate());
        record.setDescription(request.description());
        record.setResponsible(request.responsible());
        return record;
    }
}
