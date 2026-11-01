package br.com.manutencao.central_manutencao.maintenance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;
import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.MaintenancePlanService;
import br.com.manutencao.central_manutencao.plan.PlanStatus;

@ExtendWith(MockitoExtension.class)
class MaintenanceServiceTest {

    @Mock
    private MaintenanceRecordRepository maintenances;

    @Mock
    private EquipmentService equipmentService;

    @Mock
    private MaintenancePlanService planService;

    @InjectMocks
    private MaintenanceService service;

    private Equipment equipment;
    private MaintenancePlan plan;

    @BeforeEach
    void setUp() {
        // Equipamento 1 com 1300h; plano de 250h, última manutenção em 1000h (vencido desde 1250h)
        equipment = new Equipment();
        equipment.setCode("ESC-001");
        equipment.setCurrentHours(new BigDecimal("1300.0"));
        ReflectionTestUtils.setField(equipment, "id", 1L);

        plan = new MaintenancePlan();
        plan.setEquipment(equipment);
        plan.setIntervalHours(new BigDecimal("250.0"));
        plan.setWarningHours(new BigDecimal("50.0"));
        plan.setLastMaintenanceHours(new BigDecimal("1000.0"));
    }

    private MaintenanceRecord data() {
        MaintenanceRecord record = new MaintenanceRecord();
        record.setDescription("  Troca de oleo  ");
        record.setScheduledDate(LocalDate.of(2026, 11, 10));
        return record;
    }

    private MaintenanceRecord scheduledRecord(MaintenancePlan withPlan) {
        MaintenanceRecord record = data();
        record.setEquipment(equipment);
        record.setPlan(withPlan);
        record.setStatus(MaintenanceStatus.SCHEDULED);
        record.setResponsible("Carlos");
        return record;
    }

    // ---- schedule ----

    @Test
    void scheduleCreatesRecordAsScheduledWithPlan() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(planService.findById(5L)).thenReturn(plan);
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord saved = service.schedule(1L, 5L, data());

        assertEquals(MaintenanceStatus.SCHEDULED, saved.getStatus());
        assertEquals(plan, saved.getPlan());
        assertEquals(equipment, saved.getEquipment());
        assertEquals("Troca de oleo", saved.getDescription());
    }

    @Test
    void scheduleWithoutPlanCreatesStandaloneRecord() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord saved = service.schedule(1L, null, data());

        assertNull(saved.getPlan());
        assertEquals(MaintenanceStatus.SCHEDULED, saved.getStatus());
    }

    @Test
    void scheduleRejectsPlanFromAnotherEquipment() {
        Equipment other = new Equipment();
        ReflectionTestUtils.setField(other, "id", 2L);
        plan.setEquipment(other);
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(planService.findById(5L)).thenReturn(plan);

        assertThrows(BusinessRuleException.class, () -> service.schedule(1L, 5L, data()));

        verify(maintenances, never()).save(any());
    }

    @Test
    void scheduleRejectsInactivePlan() {
        plan.setActive(false);
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(planService.findById(5L)).thenReturn(plan);

        assertThrows(BusinessRuleException.class, () -> service.schedule(1L, 5L, data()));

        verify(maintenances, never()).save(any());
    }

    @Test
    void scheduleRejectsInactiveEquipment() {
        equipment.setActive(false);
        when(equipmentService.findById(1L)).thenReturn(equipment);

        assertThrows(BusinessRuleException.class, () -> service.schedule(1L, null, data()));

        verify(maintenances, never()).save(any());
    }

    // ---- complete ----

    @Test
    void completeResetsPlanCountingToTheMaintenanceHours() {
        MaintenanceRecord record = scheduledRecord(plan);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals(PlanStatus.OVERDUE, plan.status());

        MaintenanceRecord done = service.complete(7L, LocalDate.of(2026, 11, 5), new BigDecimal("1290.0"), null);

        assertEquals(MaintenanceStatus.COMPLETED, done.getStatus());
        assertEquals(new BigDecimal("1290.0"), done.getHoursAtMaintenance());
        assertEquals(LocalDate.of(2026, 11, 5), done.getPerformedAt());
        // A contagem do plano recomeçou em 1290h: próximo vencimento em 1540h
        assertEquals(new BigDecimal("1290.0"), plan.getLastMaintenanceHours());
        assertEquals(new BigDecimal("1540.0"), plan.nextDueHours());
        assertEquals(PlanStatus.ON_TIME, plan.status());
    }

    @Test
    void completeWithoutHoursUsesEquipmentCurrentHoursAndToday() {
        MaintenanceRecord record = scheduledRecord(plan);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord done = service.complete(7L, null, null, null);

        assertEquals(new BigDecimal("1300.0"), done.getHoursAtMaintenance());
        assertEquals(LocalDate.now(), done.getPerformedAt());
        assertEquals(new BigDecimal("1300.0"), plan.getLastMaintenanceHours());
    }

    @Test
    void completeKeepsResponsibleWhenNotInformedAndReplacesWhenInformed() {
        MaintenanceRecord keep = scheduledRecord(null);
        when(maintenances.findById(7L)).thenReturn(Optional.of(keep));
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));
        assertEquals("Carlos", service.complete(7L, null, null, null).getResponsible());

        MaintenanceRecord replace = scheduledRecord(null);
        when(maintenances.findById(8L)).thenReturn(Optional.of(replace));
        assertEquals("Maria", service.complete(8L, null, null, "  Maria ").getResponsible());
    }

    @Test
    void completeStandaloneMaintenanceDoesNotTouchAnyPlan() {
        MaintenanceRecord record = scheduledRecord(null);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        service.complete(7L, null, null, null);

        assertEquals(new BigDecimal("1000.0"), plan.getLastMaintenanceHours());
    }

    @Test
    void completeRejectsHoursGreaterThanEquipmentCurrentHours() {
        MaintenanceRecord record = scheduledRecord(plan);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class, () -> service.complete(7L, null, new BigDecimal("1300.1"), null));

        assertEquals(MaintenanceStatus.SCHEDULED, record.getStatus());
        assertEquals(new BigDecimal("1000.0"), plan.getLastMaintenanceHours());
        verify(maintenances, never()).save(any());
    }

    @Test
    void completeRejectsRecordThatIsNotScheduled() {
        MaintenanceRecord record = scheduledRecord(plan);
        record.setStatus(MaintenanceStatus.COMPLETED);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class, () -> service.complete(7L, null, null, null));

        assertEquals(new BigDecimal("1000.0"), plan.getLastMaintenanceHours());
        verify(maintenances, never()).save(any());
    }

    // ---- cancel ----

    @Test
    void cancelMarksScheduledRecordAsCanceledAndLeavesPlanUntouched() {
        MaintenanceRecord record = scheduledRecord(plan);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));
        when(maintenances.save(any(MaintenanceRecord.class))).thenAnswer(inv -> inv.getArgument(0));

        MaintenanceRecord canceled = service.cancel(7L);

        assertEquals(MaintenanceStatus.CANCELED, canceled.getStatus());
        assertEquals(new BigDecimal("1000.0"), plan.getLastMaintenanceHours());
    }

    @Test
    void cancelRejectsRecordThatIsNotScheduled() {
        MaintenanceRecord record = scheduledRecord(plan);
        record.setStatus(MaintenanceStatus.COMPLETED);
        when(maintenances.findById(7L)).thenReturn(Optional.of(record));

        assertThrows(BusinessRuleException.class, () -> service.cancel(7L));

        verify(maintenances, never()).save(any());
    }

    @Test
    void findByIdThrowsWhenRecordDoesNotExist() {
        when(maintenances.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
    }
}
