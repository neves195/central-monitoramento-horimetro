package br.com.manutencao.central_manutencao.maintenance;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// planId é opcional: vazio significa manutenção avulsa, sem plano
public record MaintenanceRequest(
    Long planId,
    @NotNull LocalDate scheduledDate,
    @NotBlank @Size(max = 500) String description,
    @Size(max = 100) String responsible) {}
