package br.com.manutencao.central_manutencao.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;
import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.MaintenancePlanService;

@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenances;
    private final EquipmentService equipmentService;
    private final MaintenancePlanService planService;

    public MaintenanceService(MaintenanceRecordRepository maintenances, EquipmentService equipmentService,
            MaintenancePlanService planService) {
        this.maintenances = maintenances;
        this.equipmentService = equipmentService;
        this.planService = planService;
    }

    // planId vem null quando a manutenção é avulsa
    @Transactional
    public MaintenanceRecord schedule(Long equipmentId, Long planId, MaintenanceRecord data) {

        // Se o equipamento não existe, o findById já lança 404
        Equipment equipment = equipmentService.findById(equipmentId);

        if (!equipment.isActive()) {
            throw new BusinessRuleException("Equipamento desativado: " + equipment.getCode());
        }

        if (planId != null) {
            MaintenancePlan plan = planService.findById(planId);

            // O plano precisa ser do mesmo equipamento da manutenção
            if (!Objects.equals(plan.getEquipment().getId(), equipmentId)) {
                throw new BusinessRuleException("O plano " + planId + " nao pertence ao equipamento " + equipmentId);
            }
            if (!plan.isActive()) {
                throw new BusinessRuleException("Plano desativado: " + planId);
            }
            data.setPlan(plan);
        }

        data.setDescription(data.getDescription().trim());
        data.setEquipment(equipment);
        data.setStatus(MaintenanceStatus.SCHEDULED);
        return maintenances.save(data);
    }

    @Transactional(readOnly = true)
    public List<MaintenanceRecord> listByEquipment(Long equipmentId, MaintenanceStatus status) {
        equipmentService.findById(equipmentId);

        if (status == null) {
            return maintenances.findByEquipmentIdOrderByScheduledDateDescIdDesc(equipmentId);
        }
        return maintenances.findByEquipmentIdAndStatusOrderByScheduledDateDescIdDesc(equipmentId, status);
    }

    @Transactional(readOnly = true)
    public MaintenanceRecord findById(Long id) {
        return maintenances.findById(id).orElseThrow(() -> new ResourceNotFoundException("Manutencao nao encontrada: " + id));
    }

    // performedAt, hoursAtMaintenance e responsible vazios usam os padrões descritos na conclusão
    @Transactional
    public MaintenanceRecord complete(Long id, LocalDate performedAt, BigDecimal hoursAtMaintenance, String responsible) {
        MaintenanceRecord current = findById(id);
        requireScheduled(current, "concluida");

        BigDecimal currentHours = current.getEquipment().getCurrentHours();

        // Sem horas informadas, a manutenção foi feita agora, no horímetro atual
        BigDecimal hours = hoursAtMaintenance != null ? hoursAtMaintenance : currentHours;

        // Não existe manutenção "no futuro" do horímetro
        if (hours.compareTo(currentHours) > 0) {
            throw new BusinessRuleException("As horas da manutencao (" + hours
                    + ") nao podem ser maiores que o horimetro atual (" + currentHours + ")");
        }

        current.setStatus(MaintenanceStatus.COMPLETED);
        current.setPerformedAt(performedAt != null ? performedAt : LocalDate.now());
        current.setHoursAtMaintenance(hours);
        if (responsible != null) {
            current.setResponsible(responsible.trim());
        }

        // Mesma transação: concluir zera a contagem do plano (a próxima manutenção conta a partir daqui)
        if (current.getPlan() != null) {
            current.getPlan().setLastMaintenanceHours(hours);
        }
        return maintenances.save(current);
    }

    @Transactional
    public MaintenanceRecord cancel(Long id) {
        MaintenanceRecord current = findById(id);
        requireScheduled(current, "cancelada");

        current.setStatus(MaintenanceStatus.CANCELED);
        return maintenances.save(current);
    }

    // Só manutenção agendada pode ser concluída ou cancelada
    private void requireScheduled(MaintenanceRecord record, String action) {
        if (record.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Apenas manutencao agendada pode ser " + action
                    + " (situacao atual: " + record.getStatus() + ")");
        }
    }
}
