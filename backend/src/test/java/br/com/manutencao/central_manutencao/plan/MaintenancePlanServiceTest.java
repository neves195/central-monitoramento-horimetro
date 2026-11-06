package br.com.manutencao.central_manutencao.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;

/**
 * Testa as regras do MaintenancePlanService com o repositório simulado: última manutenção padrão,
 * limites, edição, desativação e reativação.
 */
@ExtendWith(MockitoExtension.class)
class MaintenancePlanServiceTest {

    @Mock
    private MaintenancePlanRepository plans;

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private MaintenancePlanService service;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setCode("ESC-001");
        equipment.setCurrentHours(new BigDecimal("500.0"));
    }

    private MaintenancePlan data(String lastMaintenanceHours) {
        MaintenancePlan plan = new MaintenancePlan();
        plan.setName("  Troca de oleo  ");
        plan.setIntervalHours(new BigDecimal("250.0"));
        plan.setWarningHours(new BigDecimal("50.0"));
        if (lastMaintenanceHours != null) {
            plan.setLastMaintenanceHours(new BigDecimal(lastMaintenanceHours));
        }
        return plan;
    }

    @Test
    void createWithoutLastMaintenanceUsesEquipmentCurrentHours() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(plans.save(any(MaintenancePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenancePlan saved = service.create(1L, data(null));

        assertEquals(new BigDecimal("500.0"), saved.getLastMaintenanceHours());
        assertEquals("Troca de oleo", saved.getName());
        assertEquals(equipment, saved.getEquipment());
    }

    @Test
    void createKeepsLastMaintenanceWhenInformed() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(plans.save(any(MaintenancePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenancePlan saved = service.create(1L, data("300.0"));

        assertEquals(new BigDecimal("300.0"), saved.getLastMaintenanceHours());
    }

    @Test
    void createRejectsLastMaintenanceGreaterThanCurrentHours() {
        when(equipmentService.findById(1L)).thenReturn(equipment);

        assertThrows(BusinessRuleException.class, () -> service.create(1L, data("500.1")));

        verify(plans, never()).save(any());
    }

    @Test
    void createRejectsInactiveEquipment() {
        equipment.setActive(false);
        when(equipmentService.findById(1L)).thenReturn(equipment);

        assertThrows(BusinessRuleException.class, () -> service.create(1L, data(null)));

        verify(plans, never()).save(any());
    }

    @Test
    void updateWithoutLastMaintenanceKeepsCurrentValue() {
        MaintenancePlan current = data("300.0");
        when(plans.findById(1L)).thenReturn(Optional.of(current));
        when(plans.save(any(MaintenancePlan.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenancePlan changes = data(null);
        changes.setIntervalHours(new BigDecimal("500.0"));
        MaintenancePlan updated = service.update(1L, changes);

        assertEquals(new BigDecimal("500.0"), updated.getIntervalHours());
        assertEquals(new BigDecimal("300.0"), updated.getLastMaintenanceHours());
    }

    @Test
    void deactivateMarksPlanAsInactive() {
        MaintenancePlan current = data("300.0");
        when(plans.findById(1L)).thenReturn(Optional.of(current));

        service.deactivate(1L);

        assertFalse(current.isActive());
    }

    @Test
    void findByIdThrowsWhenPlanDoesNotExist() {
        when(plans.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
    }

    @Test
    void reactivateMarksPlanAsActive() {
        MaintenancePlan current = data("300.0");
        current.setEquipment(equipment);
        current.setActive(false);
        when(plans.findById(1L)).thenReturn(Optional.of(current));

        assertTrue(service.reactivate(1L).isActive());
    }

    @Test
    void reactivateRejectsPlanOfInactiveEquipment() {
        equipment.setActive(false);
        MaintenancePlan current = data("300.0");
        current.setEquipment(equipment);
        current.setActive(false);
        when(plans.findById(1L)).thenReturn(Optional.of(current));

        assertThrows(BusinessRuleException.class, () -> service.reactivate(1L));

        assertFalse(current.isActive());
    }

    @Test
    void reactivateThrowsWhenPlanDoesNotExist() {
        when(plans.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.reactivate(9L));
    }
}
