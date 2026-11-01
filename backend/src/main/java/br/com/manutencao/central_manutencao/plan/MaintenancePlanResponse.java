package br.com.manutencao.central_manutencao.plan;

import java.math.BigDecimal;

public record MaintenancePlanResponse(
    Long id,
    Long equipmentId,
    String name,
    BigDecimal intervalHours,
    BigDecimal warningHours,
    BigDecimal lastMaintenanceHours,
    BigDecimal nextDueHours,
    BigDecimal remainingHours,
    PlanStatus status,
    boolean active) {

    public static MaintenancePlanResponse from(MaintenancePlan plan) {
        return new MaintenancePlanResponse(
            plan.getId(),
            plan.getEquipment().getId(),
            plan.getName(),
            plan.getIntervalHours(),
            plan.getWarningHours(),
            plan.getLastMaintenanceHours(),
            plan.nextDueHours(),
            plan.remainingHours(),
            plan.status(),
            plan.isActive()
        );
    }
}
