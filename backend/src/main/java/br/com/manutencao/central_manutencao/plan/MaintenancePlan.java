package br.com.manutencao.central_manutencao.plan;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import br.com.manutencao.central_manutencao.equipment.Equipment;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Plano de manutenção preventiva de um equipamento (ex: troca de óleo a cada 250 horas).
 *
 * Vencimento, restante e status não são gravados: são calculados a cada consulta
 * com o horímetro atual do equipamento, então nunca ficam desatualizados.
 */
@Entity
@Table(name = "maintenance_plans")
public class MaintenancePlan {

    // ---- CAMPOS ----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // Nome do plano (ex: Troca de óleo)
    @Column(name = "name", length = 120, nullable = false)
    private String name;

    // De quantas em quantas horas a manutenção deve ser feita
    @Column(name = "interval_hours", nullable = false, precision = 12, scale = 1)
    private BigDecimal intervalHours;

    // Quantas horas antes do vencimento o plano passa a ser aviso (PRÓXIMA)
    @Column(name = "warning_hours", nullable = false, precision = 12, scale = 1)
    private BigDecimal warningHours;

    // Horímetro em que a última manutenção foi feita
    @Column(name = "last_maintenance_hours", nullable = false, precision = 12, scale = 1)
    private BigDecimal lastMaintenanceHours;

    // Exclusão lógica: o plano desativado some das listas, mas as manutenções ligadas a ele continuam
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ---- CONSTRUTORES ----

    /** Exigido pelo JPA para criar o objeto ao ler do banco. */
    public MaintenancePlan() {
    }

    // ---- REGRAS CALCULADAS ----

    /** Próximo vencimento = última manutenção + intervalo. */
    public BigDecimal nextDueHours() {
        return lastMaintenanceHours.add(intervalHours);
    }

    /** Restante = próximo vencimento - horímetro atual (negativo quando já venceu). */
    public BigDecimal remainingHours() {
        return nextDueHours().subtract(equipment.getCurrentHours());
    }

    /**
     * Situação do plano: OVERDUE se o restante é zero ou negativo, UPCOMING se está dentro da
     * faixa de aviso (restante <= warningHours) e ON_TIME nos demais casos.
     */
    public PlanStatus status() {
        BigDecimal remaining = remainingHours();

        if (remaining.signum() <= 0) {
            return PlanStatus.OVERDUE;
        }
        if (remaining.compareTo(warningHours) <= 0) {
            return PlanStatus.UPCOMING;
        }
        return PlanStatus.ON_TIME;
    }

    // ---- GETTERS E SETTERS ----

    public Long getId() {
        return id;
    }

    public Equipment getEquipment() {
        return equipment;
    }

    public void setEquipment(Equipment equipment) {
        this.equipment = equipment;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getIntervalHours() {
        return intervalHours;
    }

    public void setIntervalHours(BigDecimal intervalHours) {
        this.intervalHours = intervalHours;
    }

    public BigDecimal getWarningHours() {
        return warningHours;
    }

    public void setWarningHours(BigDecimal warningHours) {
        this.warningHours = warningHours;
    }

    public BigDecimal getLastMaintenanceHours() {
        return lastMaintenanceHours;
    }

    public void setLastMaintenanceHours(BigDecimal lastMaintenanceHours) {
        this.lastMaintenanceHours = lastMaintenanceHours;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ---- CICLO DE VIDA ----

    /** Roda antes do primeiro INSERT e preenche created_at. */
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
