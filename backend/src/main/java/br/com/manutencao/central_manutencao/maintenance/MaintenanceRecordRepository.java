package br.com.manutencao.central_manutencao.maintenance;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso aos registros de manutenção no banco. */
public interface MaintenanceRecordRepository extends JpaRepository<MaintenanceRecord, Long> {

    /** Manutenções de um equipamento, da mais recente para a mais antiga (o id desempata). */
    List<MaintenanceRecord> findByEquipmentIdOrderByScheduledDateDescIdDesc(Long equipmentId);

    /** Mesma lista, filtrada por status. */
    List<MaintenanceRecord> findByEquipmentIdAndStatusOrderByScheduledDateDescIdDesc(Long equipmentId, MaintenanceStatus status);
}
