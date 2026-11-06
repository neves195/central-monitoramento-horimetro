package br.com.manutencao.central_manutencao.maintenance;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import br.com.manutencao.central_manutencao.equipment.Equipment;
import br.com.manutencao.central_manutencao.plan.MaintenancePlan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Registro de uma manutenção de um equipamento: agendada, concluída ou cancelada.
 *
 * Pode estar ligada a um plano (preventiva) ou não (corretiva, avulsa).
 * Não tem exclusão: o cancelamento é o status CANCELED, e o registro fica no histórico.
 */
@Entity
@Table(name = "maintenance_records")
public class MaintenanceRecord {

    // ---- CAMPOS ----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // Opcional: plano que esta manutenção atende. Ao concluir, o plano recomeça a contagem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id")
    private MaintenancePlan plan;

    // Gravado como texto (SCHEDULED, COMPLETED, CANCELED) para o banco ficar legível
    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    private MaintenanceStatus status;

    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;

    // Preenchidos só na conclusão
    @Column(name = "performed_at")
    private LocalDate performedAt;

    @Column(name = "hours_at_maintenance", precision = 12, scale = 1)
    private BigDecimal hoursAtMaintenance;

    @Column(name = "description", length = 500, nullable = false)
    private String description;

    @Column(name = "responsible", length = 100)
    private String responsible;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ---- CONSTRUTORES ----

    /** Exigido pelo JPA para criar o objeto ao ler do banco. */
    public MaintenanceRecord() {
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

    public MaintenancePlan getPlan() {
        return plan;
    }

    public void setPlan(MaintenancePlan plan) {
        this.plan = plan;
    }

    public MaintenanceStatus getStatus() {
        return status;
    }

    public void setStatus(MaintenanceStatus status) {
        this.status = status;
    }

    public LocalDate getScheduledDate() {
        return scheduledDate;
    }

    public void setScheduledDate(LocalDate scheduledDate) {
        this.scheduledDate = scheduledDate;
    }

    public LocalDate getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(LocalDate performedAt) {
        this.performedAt = performedAt;
    }

    public BigDecimal getHoursAtMaintenance() {
        return hoursAtMaintenance;
    }

    public void setHoursAtMaintenance(BigDecimal hoursAtMaintenance) {
        this.hoursAtMaintenance = hoursAtMaintenance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getResponsible() {
        return responsible;
    }

    public void setResponsible(String responsible) {
        this.responsible = responsible;
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
