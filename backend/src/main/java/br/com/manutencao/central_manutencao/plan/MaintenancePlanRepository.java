package br.com.manutencao.central_manutencao.plan;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Acesso aos planos de manutenção no banco. */
public interface MaintenancePlanRepository extends JpaRepository<MaintenancePlan, Long> {

    /** Planos ativos de um equipamento. */
    List<MaintenancePlan> findByEquipmentIdAndActiveTrue(Long equipmentId);

    /** Planos ativos de equipamentos ativos, já com o equipamento carregado (evita uma consulta por plano). */
    @Query("select p from MaintenancePlan p join fetch p.equipment e where p.active = true and e.active = true")
    List<MaintenancePlan> findActiveWithActiveEquipment();
}
