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

/**
 * Regras das manutenções: só equipamento ativo recebe agendamento, o plano precisa ser do mesmo
 * equipamento e só manutenção agendada pode ser concluída ou cancelada.
 */
@Service
public class MaintenanceService {

    private final MaintenanceRecordRepository maintenances;
    private final EquipmentService equipmentService;
    private final MaintenancePlanService planService;

    /** Injeta as dependências. */
    public MaintenanceService(MaintenanceRecordRepository maintenances, EquipmentService equipmentService,
            MaintenancePlanService planService) {
        this.maintenances = maintenances;
        this.equipmentService = equipmentService;
        this.planService = planService;
    }

    /**
     * Agenda uma manutenção com status SCHEDULED.
     *
     * @param planId plano que a manutenção atende; null quando é avulsa
     * @throws ResourceNotFoundException se o equipamento ou o plano não existir
     * @throws BusinessRuleException     se o equipamento ou o plano estiver desativado, ou se o plano for de outro equipamento
     */
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

    /**
     * Lista as manutenções do equipamento, mais recentes primeiro.
     *
     * @param status filtro opcional; null traz todas
     * @throws ResourceNotFoundException se o equipamento não existir
     */
    @Transactional(readOnly = true)
    public List<MaintenanceRecord> listByEquipment(Long equipmentId, MaintenanceStatus status) {
        equipmentService.findById(equipmentId);

        if (status == null) {
            return maintenances.findByEquipmentIdOrderByScheduledDateDescIdDesc(equipmentId);
        }
        return maintenances.findByEquipmentIdAndStatusOrderByScheduledDateDescIdDesc(equipmentId, status);
    }

    /**
     * Busca uma manutenção por id.
     *
     * @throws ResourceNotFoundException se o id não existir
     */
    @Transactional(readOnly = true)
    public MaintenanceRecord findById(Long id) {
        return maintenances.findById(id).orElseThrow(() -> new ResourceNotFoundException("Manutencao nao encontrada: " + id));
    }

    /**
     * Conclui uma manutenção agendada e, se houver plano, zera a contagem dele (a próxima
     * manutenção conta a partir das horas desta).
     *
     * @param performedAt        data realizada; null vale hoje
     * @param hoursAtMaintenance horímetro na manutenção; null vale o horímetro atual do equipamento
     * @param responsible        responsável; null mantém o do agendamento
     * @throws ResourceNotFoundException se o id não existir
     * @throws BusinessRuleException     se a manutenção não estiver agendada ou as horas passarem do horímetro atual
     */
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

    /**
     * Cancela uma manutenção agendada. O registro continua no histórico com status CANCELED.
     *
     * @throws ResourceNotFoundException se o id não existir
     * @throws BusinessRuleException     se a manutenção não estiver agendada
     */
    @Transactional
    public MaintenanceRecord cancel(Long id) {
        MaintenanceRecord current = findById(id);
        requireScheduled(current, "cancelada");

        current.setStatus(MaintenanceStatus.CANCELED);
        return maintenances.save(current);
    }

    // Só manutenção agendada pode ser concluída ou cancelada; action entra na mensagem de erro
    private void requireScheduled(MaintenanceRecord record, String action) {
        if (record.getStatus() != MaintenanceStatus.SCHEDULED) {
            throw new BusinessRuleException("Apenas manutencao agendada pode ser " + action
                    + " (situacao atual: " + record.getStatus() + ")");
        }
    }
}
