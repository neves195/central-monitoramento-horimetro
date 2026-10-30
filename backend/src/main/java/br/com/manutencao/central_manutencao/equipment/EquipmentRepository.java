package br.com.manutencao.central_manutencao.equipment;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {


    //Verifica se ja existe uma maquina cadastrada com o mesmo codigo, ignorando minusculas e maisculas 
    boolean existsByCodeIgnoreCase(String code);
}
