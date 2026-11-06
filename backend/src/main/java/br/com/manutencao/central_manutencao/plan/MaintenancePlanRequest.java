package br.com.manutencao.central_manutencao.plan;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST e PUT de plano. O intervalo precisa ser maior que zero.
 * lastMaintenanceHours é opcional: na criação vazio vale o horímetro atual; na edição vazio mantém o valor.
 */
public record MaintenancePlanRequest(
    @NotBlank @Size(max = 120) String name,
    @NotNull @DecimalMin(value = "0.0", inclusive = false) @Digits(integer = 11, fraction = 1) BigDecimal intervalHours,
    @NotNull @DecimalMin("0.0") @Digits(integer = 11, fraction = 1) BigDecimal warningHours,
    @DecimalMin("0.0") @Digits(integer = 11, fraction = 1) BigDecimal lastMaintenanceHours) {}
