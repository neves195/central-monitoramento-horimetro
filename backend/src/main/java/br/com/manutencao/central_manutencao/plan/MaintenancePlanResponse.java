package br.com.manutencao.central_manutencao.plan;

import java.math.BigDecimal;

/**
 * Plano devolvido pela API, já com vencimento, restante e status calculados no momento da consulta.
 */
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

    /** Converte a entidade (com o equipamento carregado) no formato de resposta. */
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
