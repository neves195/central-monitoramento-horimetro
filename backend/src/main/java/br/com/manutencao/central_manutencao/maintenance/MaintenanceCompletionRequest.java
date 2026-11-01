package br.com.manutencao.central_manutencao.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Size;

// Tudo opcional: performedAt vazio vale hoje, hoursAtMaintenance vazio vale o horímetro atual,
// responsible vazio mantém o do agendamento
public record MaintenanceCompletionRequest(
    LocalDate performedAt,
    @DecimalMin("0.0") @Digits(integer = 11, fraction = 1) BigDecimal hoursAtMaintenance,
    @Size(max = 100) String responsible) {}
