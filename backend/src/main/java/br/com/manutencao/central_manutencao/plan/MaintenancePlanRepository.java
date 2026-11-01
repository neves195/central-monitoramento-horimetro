package br.com.manutencao.central_manutencao.plan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {

    // Planos ativos de um equipamento
    List<MaintenancePlan> findByEquipmentIdAndActiveTrue(Long equipmentId);
}
