package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.manutencao.central_manutencao.common.BusinessRuleException;
import br.com.manutencao.central_manutencao.common.ResourceNotFoundException;

/**
 * Regras de negócio dos equipamentos: código único, horímetro inicial zero e exclusão lógica.
 */
@Service
public class EquipmentService {

    private final EquipmentRepository equipments;

    /** Injeta as dependências. */
    public EquipmentService(EquipmentRepository equipments) {
        this.equipments = equipments;
    }

    /**
     * Cadastra um equipamento. O código é aparado e deve ser único (sem diferenciar maiúsculas);
     * horímetro vazio vira zero.
     *
     * @throws BusinessRuleException se já existir equipamento com o código
     */
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

    /** Lista os equipamentos ativos. */
    @Transactional(readOnly = true)
    public List<Equipment> list() {
        return equipments.findByActiveTrue();
    }

    /**
     * Busca por id. Não filtra por active: o reactivate precisa achar o equipamento desativado,
     * e o histórico (leituras e manutenções) continua consultável depois da desativação.
     *
     * @throws ResourceNotFoundException se o id não existir
     */
    @Transactional(readOnly = true)
    public Equipment findById(Long id) {
        return equipments.findById(id).orElseThrow(() -> new ResourceNotFoundException("Equipamento não encontrado: " + id));
    }

    /**
     * Atualiza código, nome e categoria. A duplicidade de código só é verificada quando o código muda.
     *
     * @throws ResourceNotFoundException se o id não existir
     * @throws BusinessRuleException     se o novo código já pertencer a outro equipamento
     */
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

    /** Exclusão lógica: marca como inativo, sem apagar leituras nem manutenções. */
    @Transactional
    public void deactivate(Long id){
        Equipment current = findById(id);
        current.setActive(false);
    }

    /** Inverso do deactivate. Chamar em equipamento já ativo não muda nada. */
    @Transactional
    public Equipment reactivate(Long id) {
        Equipment current = findById(id);
        current.setActive(true);
        return current;
    }
}
