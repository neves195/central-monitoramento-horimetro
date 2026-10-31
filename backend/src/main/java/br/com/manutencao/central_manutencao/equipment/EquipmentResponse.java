package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;

public record EquipmentResponse(Long id, String code, String name, String category, BigDecimal currentHours, boolean active) {
    
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