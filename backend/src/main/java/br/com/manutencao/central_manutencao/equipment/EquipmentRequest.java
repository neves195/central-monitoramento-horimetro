package br.com.manutencao.central_manutencao.equipment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST e PUT de equipamento. Horímetro e status não são aceitos aqui: o horímetro
 * muda só por leitura e o status por desativação/reativação.
 */
public record EquipmentRequest(
    @NotBlank @Size (max = 30) String code,
    @NotBlank @Size (max = 120) String name,
    @Size (max = 60) String category) {}