package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;

/** Equipamento devolvido pela API. */
public record EquipmentResponse(Long id, String code, String name, String category, BigDecimal currentHours, boolean active) {

    /** Converte a entidade no formato de resposta. */
    public static EquipmentResponse from (Equipment equipment) {
        return new EquipmentResponse(
            equipment.getId(),
            equipment.getCode(),
            equipment.getName(),
            equipment.getCategory(),
            equipment.getCurrentHours(),
            equipment.isActive()
        );
    }
}