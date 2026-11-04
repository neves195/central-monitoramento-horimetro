package br.com.manutencao.central_manutencao.seed;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentRepository;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;
import br.com.manutencao.central_manutencao.maintenance.MaintenanceRecord;
import br.com.manutencao.central_manutencao.maintenance.MaintenanceRecordRepository;
import br.com.manutencao.central_manutencao.maintenance.MaintenanceStatus;
import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.MaintenancePlanRepository;
import br.com.manutencao.central_manutencao.plan.PlanStatus;
import br.com.manutencao.central_manutencao.reading.HourMeterReadingRepository;

/**
 * Confere a migration V2 contra as regras calculadas: se alguém mudar os números do seed,
 * o painel de demonstração não perde um dos estados sem ninguém perceber.
 */
@SpringBootTest
@Transactional
class SampleDataTest {

    @Autowired
    private EquipmentRepository equipments;

    @Autowired
    private EquipmentService equipmentService;

    @Autowired
    private MaintenancePlanRepository plans;

    @Autowired
    private HourMeterReadingRepository readings;

    @Autowired
    private MaintenanceRecordRepository maintenances;

    @Test
    void plansCoverAllThreeStatuses() {
        Map<PlanStatus, Long> byStatus = plans.findActiveWithActiveEquipment().stream()
                .collect(Collectors.groupingBy(MaintenancePlan::status, Collectors.counting()));

        assertEquals(3, byStatus.get(PlanStatus.OVERDUE));
        assertEquals(3, byStatus.get(PlanStatus.UPCOMING));
        assertEquals(4, byStatus.get(PlanStatus.ON_TIME));
    }

    @Test
    void lastReadingMatchesCurrentHoursOfEachEquipment() {
        List<Equipment> sample = equipments.findByActiveTrue();

        assertEquals(6, sample.size());
        for (Equipment equipment : sample) {
            var history = readings.findByEquipmentIdOrderByReadAtDescIdDesc(equipment.getId());
            assertEquals(0, equipment.getCurrentHours().compareTo(history.get(0).getHours()), equipment.getCode());
        }
    }

    @Test
    void scheduledMaintenancesStayInTheFuture() {
        List<LocalDate> scheduled = maintenances.findAll().stream()
                .filter(record -> record.getStatus() == MaintenanceStatus.SCHEDULED)
                .map(MaintenanceRecord::getScheduledDate)
                .toList();

        assertEquals(2, scheduled.size());
        assertTrue(scheduled.stream().allMatch(date -> date.isAfter(LocalDate.now())));
    }

    // Protege o motivo de o seed não usar ids fixos: o próximo cadastro não pode colidir
    @Test
    void newEquipmentDoesNotCollideWithSampleIds() {
        Equipment equipment = new Equipment();
        equipment.setCode("NOVO-001");
        equipment.setName("Cadastrado depois do seed");

        assertNotNull(equipmentService.create(equipment).getId());
    }
}
