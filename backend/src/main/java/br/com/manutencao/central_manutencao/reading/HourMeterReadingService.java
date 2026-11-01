package br.com.manutencao.central_manutencao.reading;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.equipment.EquipmentService;

@Service
public class HourMeterReadingService {

    private final HourMeterReadingRepository readings;
    private final EquipmentService equipmentService;

    public HourMeterReadingService(HourMeterReadingRepository readings, EquipmentService equipmentService) {
        this.readings = readings;
        this.equipmentService = equipmentService;
    }

    @Transactional
    public HourMeterReading register(Long equipmentId, HourMeterReading data) {

        // Se o equipamento não existe, o findById já lança 404
        Equipment equipment = equipmentService.findById(equipmentId);

        if (!equipment.isActive()) {
            throw new BusinessRuleException("Equipamento desativado: " + equipment.getCode());
        }

        // O horímetro só anda para frente (igual ao atual é aceito)
        if (data.getHours().compareTo(equipment.getCurrentHours()) < 0) {
            throw new BusinessRuleException("A leitura (" + data.getHours() + ") nao pode ser menor que o horimetro atual ("
                    + equipment.getCurrentHours() + ")");
        }

        // Data da leitura vazia vira o momento do registro
        if (data.getReadAt() == null) {
            data.setReadAt(LocalDateTime.now());
        }

        data.setRegisteredBy(data.getRegisteredBy().trim());
        data.setEquipment(equipment);

        // Mesma transação: ou grava a leitura e atualiza o equipamento, ou nenhum dos dois
        equipment.setCurrentHours(data.getHours());
        return readings.save(data);
    }

    @Transactional(readOnly = true)
    public List<HourMeterReading> listByEquipment(Long equipmentId) {
        equipmentService.findById(equipmentId);
        return readings.findByEquipmentIdOrderByReadAtDescIdDesc(equipmentId);
    }
}
