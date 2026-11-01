package br.com.manutencao.central_manutencao.alert;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.MaintenancePlanRepository;
import br.com.manutencao.central_manutencao.plan.PlanStatus;

/**
 * Alertas não são gravados: são os planos ativos, de equipamentos ativos,
 * cujo status calculado agora é OVERDUE ou UPCOMING.
 */
@Service
public class AlertService {

    private final MaintenancePlanRepository plans;

    public AlertService(MaintenancePlanRepository plans) {
        this.plans = plans;
    }

    // status vem null para trazer vencidos e próximos juntos; ON_TIME nunca é alerta, então resulta em lista vazia
    @Transactional(readOnly = true)
    public List<MaintenancePlan> list(PlanStatus status) {
        return plans.findActiveWithActiveEquipment().stream()
                .filter(plan -> plan.status() != PlanStatus.ON_TIME)
                .filter(plan -> status == null || plan.status() == status)
                // Mais urgente primeiro: o restante mais baixo (ou mais negativo) vem no topo
                .sorted(Comparator.comparing(MaintenancePlan::remainingHours))
                .toList();
    }
}
