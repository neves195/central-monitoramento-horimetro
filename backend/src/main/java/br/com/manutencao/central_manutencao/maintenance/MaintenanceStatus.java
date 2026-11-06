package br.com.manutencao.central_manutencao.maintenance;

/**
 * Situação de uma manutenção. Só SCHEDULED pode mudar:
 * ou vira COMPLETED (foi feita) ou CANCELED (não será mais feita).
 */
public enum MaintenanceStatus {

    // Agendada, ainda não realizada
    SCHEDULED,

    // Realizada: tem data e horímetro de conclusão
    COMPLETED,

    // Desistência: o registro fica no histórico
    CANCELED
}
