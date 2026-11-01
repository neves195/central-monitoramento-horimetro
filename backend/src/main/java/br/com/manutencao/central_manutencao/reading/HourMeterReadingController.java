package br.com.manutencao.central_manutencao.reading;

import java.net.URI;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/equipments/{equipmentId}/readings")
public class HourMeterReadingController {

    private final HourMeterReadingService service;

    public HourMeterReadingController(HourMeterReadingService service) {
        this.service = service;
    }

    // POST /api/equipments/{equipmentId}/readings -> 201, atualiza o horímetro do equipamento
    @PostMapping
    public ResponseEntity<HourMeterReadingResponse> register(@PathVariable Long equipmentId,
            @Valid @RequestBody HourMeterReadingRequest request) {
        HourMeterReading created = service.register(equipmentId, toEntity(request));
        URI location = URI.create("/api/equipments/" + equipmentId + "/readings");
        return ResponseEntity.created(location).body(HourMeterReadingResponse.from(created));
    }

    // GET /api/equipments/{equipmentId}/readings -> histórico, mais recente primeiro
    @GetMapping
    public List<HourMeterReadingResponse> list(@PathVariable Long equipmentId) {
        return service.listByEquipment(equipmentId).stream().map(HourMeterReadingResponse::from).toList();
    }

    // O equipamento vem do caminho da URL, não do corpo
    private HourMeterReading toEntity(HourMeterReadingRequest request) {
        HourMeterReading reading = new HourMeterReading();
        reading.setHours(request.hours());
        reading.setReadAt(request.readAt());
        reading.setRegisteredBy(request.registeredBy());
        return reading;
    }
}
