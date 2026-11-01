package br.com.manutencao.central_manutencao.reading;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HourMeterReadingRepository extends JpaRepository<HourMeterReading, Long> {

    // Histórico de um equipamento, da leitura mais recente para a mais antiga
    List<HourMeterReading> findByEquipmentIdOrderByReadAtDescIdDesc(Long equipmentId);
}
