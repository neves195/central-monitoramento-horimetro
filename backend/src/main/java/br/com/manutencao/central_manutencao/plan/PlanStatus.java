package br.com.manutencao.central_manutencao.plan;

/**
 * Situação de um plano de manutenção. Nunca é gravada no banco:
 * é calculada na hora, a partir do horímetro atual do equipamento.
 */
public enum PlanStatus {

    // Restante <= 0: o horímetro já chegou ou passou do vencimento
    OVERDUE,

    // Restante dentro da faixa de aviso (<= warningHours)
    UPCOMING,

    // Ainda longe do vencimento
    ON_TIME
}
