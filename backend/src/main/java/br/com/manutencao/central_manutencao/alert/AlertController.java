package br.com.manutencao.central_manutencao.alert;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.manutencao.central_manutencao.plan.PlanStatus;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService service;

    public AlertController(AlertService service) {
        this.service = service;
    }

    // GET /api/alerts?status=OVERDUE -> planos vencidos e próximos, mais urgentes primeiro (status é opcional)
    @GetMapping
    public List<AlertResponse> list(@RequestParam(required = false) PlanStatus status) {
        return service.list(status).stream().map(AlertResponse::from).toList();
    }
}
