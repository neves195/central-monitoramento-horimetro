package br.com.manutencao.central_manutencao.reading;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HourMeterReadingResponse(Long id, Long equipmentId, BigDecimal hours, LocalDateTime readAt, String registeredBy) {

    public static HourMeterReadingResponse from(HourMeterReading reading) {
        return new HourMeterReadingResponse(
            reading.getId(),
            reading.getEquipment().getId(),
            reading.getHours(),
            reading.getReadAt(),
            reading.getRegisteredBy()
        );
    }
}
