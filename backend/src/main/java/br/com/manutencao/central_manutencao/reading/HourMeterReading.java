package br.com.manutencao.central_manutencao.reading;

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
 * Leitura do horímetro de um equipamento, registrada por alguém numa data.
 *
 * É o histórico: o horímetro atual do equipamento é atualizado junto com cada leitura.
 * Leituras nunca são apagadas nem alteradas.
 */
@Entity
@Table(name = "hour_meter_readings")
public class HourMeterReading {

    // ---- CAMPOS ----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    // Valor lido no horímetro, com 1 casa decimal (ex: 1234.5)
    @Column(name = "hours", nullable = false, precision = 12, scale = 1)
    private BigDecimal hours;

    // Quando a leitura foi feita (pode ser diferente de quando foi registrada)
    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    // Quem anotou a leitura
    @Column(name = "registered_by", length = 50, nullable = false)
    private String registeredBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ---- CONSTRUTORES ----

    // Exigido pelo JPA para criar o objeto ao ler do banco
    public HourMeterReading() {
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

    public BigDecimal getHours() {
        return hours;
    }

    public void setHours(BigDecimal hours) {
        this.hours = hours;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }

    public String getRegisteredBy() {
        return registeredBy;
    }

    public void setRegisteredBy(String registeredBy) {
        this.registeredBy = registeredBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ---- CICLO DE VIDA ----

    // Roda automaticamente antes de gravar pela primeira vez
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
