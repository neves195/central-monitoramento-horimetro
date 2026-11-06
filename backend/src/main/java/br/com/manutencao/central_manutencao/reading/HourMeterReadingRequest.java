package br.com.manutencao.central_manutencao.reading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Corpo de POST de leitura. O valor aceita no máximo 1 casa decimal.
 * readAt é opcional: se vier vazio, vale o momento do registro.
 */
public record HourMeterReadingRequest(
    @NotNull @DecimalMin("0.0") @Digits(integer = 11, fraction = 1) BigDecimal hours,
    LocalDateTime readAt,
    @NotBlank @Size(max = 50) String registeredBy) {}
