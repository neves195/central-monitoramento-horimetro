package br.com.manutencao.central_manutencao.maintenance;

/**
 * Situação de uma manutenção. Só SCHEDULED pode mudar:
 * ou vira COMPLETED (foi feita) ou CANCELED (não será mais feita).
 */
public enum MaintenanceStatus {

    SCHEDULED,

    COMPLETED,

    CANCELED
}
