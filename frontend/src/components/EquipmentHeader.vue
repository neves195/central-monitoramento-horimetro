<script setup>
import { formatHours } from '../format'

defineProps({
  id: { type: String, required: true },
  equipment: { type: Object, default: null },
})
</script>

<template>
  <div class="equipment-header">
    <RouterLink to="/equipamentos" class="back">← Equipamentos</RouterLink>

    <div class="page-title">
      <div>
        <h1>{{ equipment?.code ?? 'Equipamento' }}</h1>
        <p v-if="equipment" class="muted">{{ equipment.name }}</p>
      </div>
      <div v-if="equipment" class="hours">
        <span class="muted">Horímetro atual</span>
        <strong>{{ formatHours(equipment.currentHours) }}</strong>
      </div>
    </div>

    <nav class="tabs" aria-label="Dados do equipamento">
      <RouterLink :to="{ name: 'readings', params: { id } }">Leituras</RouterLink>
      <RouterLink :to="{ name: 'plans', params: { id } }">Planos</RouterLink>
      <RouterLink :to="{ name: 'maintenances', params: { id } }">Manutenções</RouterLink>
    </nav>
  </div>
</template>

<style scoped>
.equipment-header {
  display: grid;
  gap: 12px;
}

.back {
  font-size: 0.9rem;
}

.hours {
  display: grid;
  text-align: right;
}

.hours .muted {
  font-size: 0.8rem;
}

.hours strong {
  font-size: 1.3rem;
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  border-bottom: 1px solid var(--line);
}

.tabs a {
  padding: 8px 14px;
  border-bottom: 3px solid transparent;
  color: var(--muted);
  font-weight: 600;
  text-decoration: none;
}

.tabs a:hover {
  color: var(--ink);
}

.tabs a.router-link-active {
  border-bottom-color: var(--brand);
  color: var(--brand);
}
</style>
