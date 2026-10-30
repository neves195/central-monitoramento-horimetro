package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;

@Service
public class EquipmentService {

    private final EquipmentRepository equipments;

    public EquipmentService(EquipmentRepository equipments) {
        this.equipments = equipments;
    }

    //  Tirar os espaços das pontas do código (trim).
    // Perguntar se já existe equipamento com esse código; se existir, lançar BusinessRuleException com uma mensagem clara.
    // Se o horímetro atual vier vazio, usar zero.
    //  Gravar com save e devolver o resultado.
    public Equipment create(Equipment equipment) {

        // Tira os espaços das pontas do código
        String code = equipment.getCode().trim();
        equipment.setCode(code);

        // Se ja existe retorna erro
        if (equipments.existsByCodeIgnoreCase(code)) {
            throw new BusinessRuleException("Equipamento já cadastrado!");
        }

        // Horímetro vazio vira zero
        if (equipment.getCurrentHours() == null) {
            equipment.setCurrentHours(BigDecimal.ZERO);
        }

        return equipments.save(equipment);
    }
}
