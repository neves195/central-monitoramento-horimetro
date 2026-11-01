package br.com.manutencao.central_manutencao.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.manutencao.central_manutencao.equipment.Equipment;

// Plano de 250h, última manutenção em 1000h (vence em 1250h), aviso nas últimas 50h
class MaintenancePlanTest {

    private Equipment equipment;
    private MaintenancePlan plan;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        plan = new MaintenancePlan();
        plan.setEquipment(equipment);
        plan.setIntervalHours(new BigDecimal("250.0"));
        plan.setWarningHours(new BigDecimal("50.0"));
        plan.setLastMaintenanceHours(new BigDecimal("1000.0"));
    }

    private void currentHours(String hours) {
        equipment.setCurrentHours(new BigDecimal(hours));
    }

    @Test
    void nextDueIsLastMaintenancePlusInterval() {
        currentHours("1100.0");

        assertEquals(new BigDecimal("1250.0"), plan.nextDueHours());
    }

    @Test
    void remainingIsNextDueMinusCurrentHours() {
        currentHours("1100.0");

        assertEquals(new BigDecimal("150.0"), plan.remainingHours());
    }

    @Test
    void statusIsOnTimeWhenRemainingIsAboveWarning() {
        currentHours("1199.9");

        assertEquals(PlanStatus.ON_TIME, plan.status());
    }

    @Test
    void statusIsUpcomingWhenRemainingEqualsWarning() {
        currentHours("1200.0");

        assertEquals(PlanStatus.UPCOMING, plan.status());
    }

    @Test
    void statusIsUpcomingWhenRemainingIsPositiveAndInsideWarning() {
        currentHours("1249.9");

        assertEquals(PlanStatus.UPCOMING, plan.status());
    }

    @Test
    void statusIsOverdueWhenRemainingIsZero() {
        currentHours("1250.0");

        assertEquals(PlanStatus.OVERDUE, plan.status());
    }

    @Test
    void statusIsOverdueWhenRemainingIsNegative() {
        currentHours("1300.0");

        assertEquals(PlanStatus.OVERDUE, plan.status());
        assertEquals(new BigDecimal("-50.0"), plan.remainingHours());
    }
}
