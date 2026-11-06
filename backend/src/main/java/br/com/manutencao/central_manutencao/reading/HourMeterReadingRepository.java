package br.com.manutencao.central_manutencao.reading;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso às leituras de horímetro no banco. */
public interface HourMeterReadingRepository extends JpaRepository<HourMeterReading, Long> {

    /** Histórico de um equipamento, da leitura mais recente para a mais antiga (o id desempata). */
    List<HourMeterReading> findByEquipmentIdOrderByReadAtDescIdDesc(Long equipmentId);
}
