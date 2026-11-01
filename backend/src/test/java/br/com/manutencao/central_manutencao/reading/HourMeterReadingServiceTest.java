package br.com.manutencao.central_manutencao.reading;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;

@ExtendWith(MockitoExtension.class)
class HourMeterReadingServiceTest {

    @Mock
    private HourMeterReadingRepository readings;

    @Mock
    private EquipmentService equipmentService;

    @InjectMocks
    private HourMeterReadingService service;

    private Equipment equipment;

    @BeforeEach
    void setUp() {
        equipment = new Equipment();
        equipment.setCode("ESC-001");
        equipment.setCurrentHours(new BigDecimal("100.0"));
    }

    private HourMeterReading reading(String hours) {
        HourMeterReading reading = new HourMeterReading();
        reading.setHours(new BigDecimal(hours));
        reading.setRegisteredBy("  Joao  ");
        return reading;
    }

    @Test
    void registerUpdatesEquipmentCurrentHours() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(readings.save(any(HourMeterReading.class))).thenAnswer(inv -> inv.getArgument(0));

        HourMeterReading saved = service.register(1L, reading("150.5"));

        assertEquals(new BigDecimal("150.5"), equipment.getCurrentHours());
        assertEquals("Joao", saved.getRegisteredBy());
        assertEquals(equipment, saved.getEquipment());
    }

    @Test
    void registerAcceptsReadingEqualToCurrentHours() {
        when(equipmentService.findById(1L)).thenReturn(equipment);
        when(readings.save(any(HourMeterReading.class))).thenAnswer(inv -> inv.getArgument(0));

        service.register(1L, reading("100.0"));

        assertEquals(new BigDecimal("100.0"), equipment.getCurrentHours());
    }

    @Test
    void registerRejectsReadingLowerThanCurrentHours() {
        when(equipmentService.findById(1L)).thenReturn(equipment);

        assertThrows(BusinessRuleException.class, () -> service.register(1L, reading("99.9")));

        assertEquals(new BigDecimal("100.0"), equipment.getCurrentHours());
        verify(readings, never()).save(any());
    }

    @Test
    void registerRejectsInactiveEquipment() {
        equipment.setActive(false);
        when(equipmentService.findById(1L)).thenReturn(equipment);

        assertThrows(BusinessRuleException.class, () -> service.register(1L, reading("150.0")));

        verify(readings, never()).save(any());
    }
}
