package br.com.manutencao.central_manutencao.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EquipmentRequest(
    @NotBlank @Size (max = 30) String code,
    @NotBlank @Size (max = 120) String name,
    @Size (max = 60) String category) {}