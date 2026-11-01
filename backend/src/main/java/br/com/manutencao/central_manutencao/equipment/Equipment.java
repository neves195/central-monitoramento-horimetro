package br.com.manutencao.central_manutencao.equipment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

/**
 * Equipamento monitorado pelo sistema (escavadeira, gerador, etc.).
 *
 * O horímetro atual fica guardado aqui e é atualizado junto com cada leitura
 * registrada, para os alertas não precisarem consultar o histórico.
 */
@Entity
@Table(name = "equipments")
public class Equipment {

    // ---- CAMPOS ----

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Identificador único do equipamento na empresa (ex: ESC-001, GER-001)
    @Column(name = "code", length = 30, nullable = false, unique = true)
    private String code;

    // Nome do equipamento (ex: Escavadeira CAT, Gerador)
    @Column(name = "name", length = 120, nullable = false)
    private String name;

    // Opcional: tipo do equipamento (ex: escavadeira, gerador)
    @Column(name = "category", length = 60)
    private String category;

    // Horímetro atual, com 1 casa decimal (ex: 1234.5). Muda apenas por leitura
    @Column(name = "current_hours", nullable = false, precision = 12, scale = 1)
    private BigDecimal currentHours;

    // Exclusão lógica: desativado em vez de apagado, para preservar o histórico de leituras e alertas
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // ---- CONSTRUTORES ----

    // Exigido pelo JPA para criar o objeto ao ler do banco
    public Equipment() {
    }

    // ---- GETTERS E SETTERS ----

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public BigDecimal getCurrentHours() {
        return currentHours;
    }

    public void setCurrentHours(BigDecimal currentHours) {
        this.currentHours = currentHours;
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

    // ---- CICLO DE VIDA ----s

    // Roda automaticamente antes de gravar pela primeira vez.
    // Garante que created_at nunca fique vazio, não importa quem criou o equipamento.
    @PrePersist
    void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}