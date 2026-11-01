package br.com.manutencao.central_manutencao.equipment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class EquipmentServiceTest {

    @Mock
    private EquipmentRepository equipments;

    @InjectMocks
    private EquipmentService service;

    private Equipment equipment(String code, String name) {
        Equipment equipment = new Equipment();
        equipment.setCode(code);
        equipment.setName(name);
        return equipment;
    }

    // ---- create ----

    @Test
    void createTrimsCodeAndDefaultsHoursToZero() {
        when(equipments.existsByCodeIgnoreCase("ESC-001")).thenReturn(false);
        when(equipments.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipment saved = service.create(equipment("  ESC-001  ", "Escavadeira"));

        assertEquals("ESC-001", saved.getCode());
        assertEquals(BigDecimal.ZERO, saved.getCurrentHours());
    }

    @Test
    void createKeepsHoursWhenInformed() {
        when(equipments.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));
        Equipment data = equipment("ESC-001", "Escavadeira");
        data.setCurrentHours(new BigDecimal("120.5"));

        assertEquals(new BigDecimal("120.5"), service.create(data).getCurrentHours());
    }

    @Test
    void createRejectsDuplicateCode() {
        when(equipments.existsByCodeIgnoreCase("ESC-001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.create(equipment("ESC-001", "Escavadeira")));

        verify(equipments, never()).save(any());
    }

    // ---- list e findById ----

    @Test
    void listReturnsOnlyActiveEquipments() {
        List<Equipment> active = List.of(equipment("ESC-001", "Escavadeira"));
        when(equipments.findByActiveTrue()).thenReturn(active);

        assertEquals(active, service.list());
    }

    @Test
    void findByIdThrowsWhenEquipmentDoesNotExist() {
        when(equipments.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(9L));
    }

    // ---- update ----

    @Test
    void updateChangesNameCategoryAndCode() {
        Equipment current = equipment("ESC-001", "Escavadeira");
        when(equipments.findById(1L)).thenReturn(Optional.of(current));
        when(equipments.existsByCodeIgnoreCase("ESC-002")).thenReturn(false);
        when(equipments.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipment data = equipment("  ESC-002 ", "Escavadeira CAT");
        data.setCategory("escavadeira");
        Equipment updated = service.update(1L, data);

        assertEquals("ESC-002", updated.getCode());
        assertEquals("Escavadeira CAT", updated.getName());
        assertEquals("escavadeira", updated.getCategory());
    }

    @Test
    void updateDoesNotCheckDuplicateWhenCodeOnlyChangesCase() {
        Equipment current = equipment("ESC-001", "Escavadeira");
        when(equipments.findById(1L)).thenReturn(Optional.of(current));
        when(equipments.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        // Mesmo código com outra caixa: não é um código novo, então não consulta duplicidade
        service.update(1L, equipment("esc-001", "Escavadeira"));

        verify(equipments, never()).existsByCodeIgnoreCase(any());
    }

    @Test
    void updateRejectsCodeOfAnotherEquipment() {
        Equipment current = equipment("ESC-001", "Escavadeira");
        when(equipments.findById(1L)).thenReturn(Optional.of(current));
        when(equipments.existsByCodeIgnoreCase("GER-001")).thenReturn(true);

        assertThrows(BusinessRuleException.class, () -> service.update(1L, equipment("GER-001", "Gerador")));

        assertEquals("ESC-001", current.getCode());
        verify(equipments, never()).save(any());
    }

    @Test
    void updateNeverChangesCurrentHours() {
        Equipment current = equipment("ESC-001", "Escavadeira");
        current.setCurrentHours(new BigDecimal("500.0"));
        when(equipments.findById(1L)).thenReturn(Optional.of(current));
        when(equipments.save(any(Equipment.class))).thenAnswer(inv -> inv.getArgument(0));

        Equipment data = equipment("ESC-001", "Escavadeira");
        data.setCurrentHours(new BigDecimal("1.0"));
        service.update(1L, data);

        // O horímetro só muda por leitura
        assertEquals(new BigDecimal("500.0"), current.getCurrentHours());
    }

    // ---- deactivate ----

    @Test
    void deactivateMarksEquipmentAsInactive() {
        Equipment current = equipment("ESC-001", "Escavadeira");
        when(equipments.findById(1L)).thenReturn(Optional.of(current));

        service.deactivate(1L);

        assertFalse(current.isActive());
        verify(equipments, never()).delete(any());
    }

    @Test
    void deactivateThrowsWhenEquipmentDoesNotExist() {
        when(equipments.findById(9L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deactivate(9L));
    }
}
