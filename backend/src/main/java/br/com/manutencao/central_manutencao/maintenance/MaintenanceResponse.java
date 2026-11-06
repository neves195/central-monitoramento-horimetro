package br.com.manutencao.central_manutencao.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Manutenção devolvida pela API. planId é null em manutenção avulsa. */
public record MaintenanceResponse(
    Long id,
    Long equipmentId,
    Long planId,
    MaintenanceStatus status,
    LocalDate scheduledDate,
    LocalDate performedAt,
    BigDecimal hoursAtMaintenance,
    String description,
    String responsible) {

    /** Converte a entidade no formato de resposta. */
    public static MaintenanceResponse from(MaintenanceRecord record) {
        return new MaintenanceResponse(
            record.getId(),
            record.getEquipment().getId(),
            // Manutenção avulsa não tem plano
            record.getPlan() == null ? null : record.getPlan().getId(),
            record.getStatus(),
            record.getScheduledDate(),
            record.getPerformedAt(),
            record.getHoursAtMaintenance(),
            record.getDescription(),
            record.getResponsible()
        );
    }
}
