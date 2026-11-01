package br.com.manutencao.central_manutencao.alert;

import java.math.BigDecimal;

import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.PlanStatus;

// Um plano que precisa de atenção, com os dados do equipamento para exibir sem outra consulta
public record AlertResponse(
    Long planId,
    String planName,
    Long equipmentId,
    String equipmentCode,
    String equipmentName,
    BigDecimal currentHours,
    BigDecimal nextDueHours,
    BigDecimal remainingHours,
    PlanStatus status) {

    public static AlertResponse from(MaintenancePlan plan) {
        return new AlertResponse(
            plan.getId(),
            plan.getName(),
            plan.getEquipment().getId(),
            plan.getEquipment().getCode(),
            plan.getEquipment().getName(),
            plan.getEquipment().getCurrentHours(),
            plan.nextDueHours(),
            plan.remainingHours(),
            plan.status()
        );
    }
}
