package br.com.manutencao.central_manutencao.plan;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;

/**
 * Regras dos planos de manutenção: só equipamento ativo recebe plano e a última manutenção
 * nunca passa do horímetro atual.
 */
@Service
public class MaintenancePlanService {

    private final MaintenancePlanRepository plans;
    private final EquipmentService equipmentService;

    /** Injeta as dependências. */
    public MaintenancePlanService(MaintenancePlanRepository plans, EquipmentService equipmentService) {
        this.plans = plans;
        this.equipmentService = equipmentService;
    }

    /**
     * Cria um plano para o equipamento. Sem última manutenção informada, a contagem começa no horímetro atual.
     *
     * @throws ResourceNotFoundException se o equipamento não existir
     * @throws BusinessRuleException     se o equipamento estiver desativado ou a última manutenção for maior que o horímetro atual
     */
    @Transactional
    public MaintenancePlan create(Long equipmentId, MaintenancePlan data) {

        // Se o equipamento não existe, o findById já lança 404
        Equipment equipment = equipmentService.findById(equipmentId);

        if (!equipment.isActive()) {
            throw new BusinessRuleException("Equipamento desativado: " + equipment.getCode());
        }

        // Sem última manutenção informada, a contagem começa no horímetro atual
        if (data.getLastMaintenanceHours() == null) {
            data.setLastMaintenanceHours(equipment.getCurrentHours());
        }

        // Não existe manutenção "no futuro" do horímetro
        if (data.getLastMaintenanceHours().compareTo(equipment.getCurrentHours()) > 0) {
            throw new BusinessRuleException("A ultima manutencao (" + data.getLastMaintenanceHours()
                    + ") nao pode ser maior que o horimetro atual (" + equipment.getCurrentHours() + ")");
        }

        data.setName(data.getName().trim());
        data.setEquipment(equipment);
        return plans.save(data);
    }

    /**
     * Lista os planos ativos do equipamento.
     *
     * @throws ResourceNotFoundException se o equipamento não existir
     */
    @Transactional(readOnly = true)
    public List<MaintenancePlan> listByEquipment(Long equipmentId) {
        equipmentService.findById(equipmentId);
        return plans.findByEquipmentIdAndActiveTrue(equipmentId);
    }

    /**
     * Busca um plano por id, ativo ou não.
     *
     * @throws ResourceNotFoundException se o id não existir
     */
    @Transactional(readOnly = true)
    public MaintenancePlan findById(Long id) {
        return plans.findById(id).orElseThrow(() -> new ResourceNotFoundException("Plano nao encontrado: " + id));
    }

    /**
     * Atualiza nome, intervalo e aviso. A última manutenção só muda se for informada.
     *
     * @throws ResourceNotFoundException se o id não existir
     */
    @Transactional
    public MaintenancePlan update(Long id, MaintenancePlan data) {
        MaintenancePlan current = findById(id);

        current.setName(data.getName().trim());
        current.setIntervalHours(data.getIntervalHours());
        current.setWarningHours(data.getWarningHours());

        // Vazio mantém a última manutenção atual
        if (data.getLastMaintenanceHours() != null) {
            current.setLastMaintenanceHours(data.getLastMaintenanceHours());
        }
        return plans.save(current);
    }

    /**
     * Exclusão lógica: o plano some das listas, mas as manutenções ligadas a ele continuam.
     *
     * @throws ResourceNotFoundException se o id não existir
     */
    @Transactional
    public void deactivate(Long id) {
        MaintenancePlan current = findById(id);
        current.setActive(false);
    }

    /**
     * Reativa o plano. Plano ativo em equipamento desativado não faz sentido: mesma regra do create.
     *
     * @throws ResourceNotFoundException se o id não existir
     * @throws BusinessRuleException     se o equipamento estiver desativado
     */
    @Transactional
    public MaintenancePlan reactivate(Long id) {
        MaintenancePlan current = findById(id);

        if (!current.getEquipment().isActive()) {
            throw new BusinessRuleException("Equipamento desativado: " + current.getEquipment().getCode());
        }

        current.setActive(true);
        return current;
    }
}
