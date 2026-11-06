package br.com.manutencao.central_manutencao.alert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import br.com.manutencao.central_manutencao.plan.MaintenancePlanRepository;
import br.com.manutencao.central_manutencao.plan.PlanStatus;

/**
 * Testa o AlertService com o repositório simulado: ON_TIME fica fora, a ordem é do mais urgente
 * para o menos urgente e o filtro por status funciona.
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private MaintenancePlanRepository plans;

    @InjectMocks
    private AlertService service;

    // Plano de 250h com última manutenção em 0h (vence em 250h) e aviso nas últimas 50h
    private MaintenancePlan plan(String name, String currentHours) {
        Equipment equipment = new Equipment();
        equipment.setCurrentHours(new BigDecimal(currentHours));

        MaintenancePlan plan = new MaintenancePlan();
        plan.setEquipment(equipment);
        plan.setName(name);
        plan.setIntervalHours(new BigDecimal("250.0"));
        plan.setWarningHours(new BigDecimal("50.0"));
        plan.setLastMaintenanceHours(BigDecimal.ZERO);
        return plan;
    }

    @Test
    void listIgnoresPlansThatAreOnTime() {
        when(plans.findActiveWithActiveEquipment()).thenReturn(List.of(plan("em dia", "100.0")));

        assertTrue(service.list(null).isEmpty());
    }

    @Test
    void listReturnsOverdueAndUpcomingMostUrgentFirst() {
        MaintenancePlan upcoming = plan("proximo", "220.0");
        MaintenancePlan overdue = plan("vencido", "260.0");
        MaintenancePlan onTime = plan("em dia", "10.0");
        MaintenancePlan veryOverdue = plan("muito vencido", "400.0");
        when(plans.findActiveWithActiveEquipment()).thenReturn(List.of(upcoming, overdue, onTime, veryOverdue));

        List<MaintenancePlan> result = service.list(null);

        assertEquals(List.of("muito vencido", "vencido", "proximo"),
                result.stream().map(MaintenancePlan::getName).toList());
    }

    @Test
    void listFiltersByStatus() {
        MaintenancePlan upcoming = plan("proximo", "220.0");
        MaintenancePlan overdue = plan("vencido", "260.0");
        when(plans.findActiveWithActiveEquipment()).thenReturn(List.of(upcoming, overdue));

        assertEquals(List.of(overdue), service.list(PlanStatus.OVERDUE));
        assertEquals(List.of(upcoming), service.list(PlanStatus.UPCOMING));
    }

    @Test
    void listWithOnTimeStatusIsAlwaysEmpty() {
        when(plans.findActiveWithActiveEquipment()).thenReturn(List.of(plan("em dia", "10.0"), plan("vencido", "260.0")));

        assertTrue(service.list(PlanStatus.ON_TIME).isEmpty());
    }
}
