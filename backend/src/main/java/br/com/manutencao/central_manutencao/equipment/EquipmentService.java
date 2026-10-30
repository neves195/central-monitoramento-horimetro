package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;

@Service
public class EquipmentService {

    private final EquipmentRepository equipments;

    public EquipmentService(EquipmentRepository equipments) {
        this.equipments = equipments;
    }

    @Transactional
    public Equipment create(Equipment equipment) {

        // Tira os espaços das pontas do código
        String code = equipment.getCode().trim();
        equipment.setCode(code);

        // Se ja existe retorna erro
        if (equipments.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Equipamento já cadastrado: " + code);
        }

        // Horímetro vazio vira zero
        if (equipment.getCurrentHours() == null) {
            equipment.setCurrentHours(BigDecimal.ZERO);
        }

        return equipments.save(equipment);
    }

    @Transactional(readOnly = true)
    public List<Equipment> list() {
        return equipments.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public Equipment findById(Long id) {
        return equipments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
    }

    @Transactional
    public Equipment update(Long id, Equipment data) {

        // Busca o equipamento atual
        Equipment current = findById(id);

        String newCode = data.getCode().trim();

        // So valida duplicidade se o codigo mudou
        if (!current.getCode().equalsIgnoreCase(newCode) && equipments.existsByCodeIgnoreCase(newCode)) {
            throw new BusinessRuleException("Ja existe um equipamento com o codigo " + newCode);
        }

        current.setCode(newCode);
        current.setName(data.getName());
        current.setCategory(data.getCategory());
        return equipments.save(current);
    }
}
