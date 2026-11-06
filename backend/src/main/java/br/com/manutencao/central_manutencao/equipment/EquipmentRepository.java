package br.com.manutencao.central_manutencao.equipment;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Acesso aos equipamentos no banco. */
public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    /** Indica se já existe equipamento com o código, ignorando maiúsculas e minúsculas. */
    boolean existsByCodeIgnoreCase(String code);

    /** Lista os equipamentos ativos. */
    List<Equipment> findByActiveTrue();
}
