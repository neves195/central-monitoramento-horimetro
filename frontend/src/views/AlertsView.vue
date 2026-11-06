<!--
  Painel de alertas (tela inicial): planos vencidos e próximos, mais urgente primeiro, com filtro por status e atalho para agendar a manutenção.
-->
<script setup>
import { computed, onMounted, ref } from 'vue'
import { api } from '../api/client'
import StatusBadge from '../components/StatusBadge.vue'
import { formatHours } from '../format'
import { auth } from '../stores/auth'

const alerts = ref([])
const filter = ref('ALL')
const loading = ref(true)
const error = ref('')

const overdueCount = computed(() => alerts.value.filter((alert) => alert.status === 'OVERDUE').length)
const upcomingCount = computed(() => alerts.value.filter((alert) => alert.status === 'UPCOMING').length)

// A ordem vem do backend (mais urgente primeiro), e o filtro só esconde itens
const visibleAlerts = computed(() =>
  filter.value === 'ALL' ? alerts.value : alerts.value.filter((alert) => alert.status === filter.value),
)

// Busca os alertas no backend
async function load() {
  loading.value = true
  error.value = ''
  try {
    alerts.value = await api('/alerts')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
}

// Texto do restante: "Vencido há X" quando passou do vencimento, "Faltam X" caso contrário
function remainingText(alert) {
  return alert.remainingHours <= 0
    ? `Vencido há ${formatHours(-alert.remainingHours)}`
    : `Faltam ${formatHours(alert.remainingHours)}`
}

// Carrega os alertas ao abrir a tela
onMounted(load)
</script>

<template>
  <div class="page-title">
    <h1>Painel de alertas</h1>
    <button type="button" class="button-secondary" :disabled="loading" @click="load">Atualizar</button>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>

  <div class="filters" role="group" aria-label="Filtrar alertas">
    <button type="button" class="tile" :class="{ selected: filter === 'ALL' }" @click="filter = 'ALL'">
      <strong>{{ alerts.length }}</strong> Todos
    </button>
    <button type="button" class="tile tile-red" :class="{ selected: filter === 'OVERDUE' }" @click="filter = 'OVERDUE'">
      <strong>{{ overdueCount }}</strong> Vencidos
    </button>
    <button
      type="button"
      class="tile tile-amber"
      :class="{ selected: filter === 'UPCOMING' }"
      @click="filter = 'UPCOMING'"
    >
      <strong>{{ upcomingCount }}</strong> Próximos
    </button>
  </div>

  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!error && visibleAlerts.length === 0" class="card empty">Nenhum alerta nesta situação.</p>

  <ul v-else class="alert-list">
    <li
      v-for="alert in visibleAlerts"
      :key="alert.planId"
      class="card alert"
      :class="alert.status === 'OVERDUE' ? 'alert-red' : 'alert-amber'"
    >
      <div class="alert-main">
        <div class="alert-title">
          <StatusBadge :status="alert.status" />
          <h2>{{ alert.planName }}</h2>
        </div>
        <p class="muted">{{ alert.equipmentCode }} · {{ alert.equipmentName }}</p>
      </div>

      <div class="alert-hours">
        <strong>{{ remainingText(alert) }}</strong>
        <span class="muted">Horímetro {{ formatHours(alert.currentHours) }} · vence em {{ formatHours(alert.nextDueHours) }}</span>
      </div>

      <RouterLink
        v-if="auth.isAdmin"
        class="alert-action"
        :to="{ name: 'maintenances', params: { id: alert.equipmentId }, query: { planId: alert.planId } }"
      >
        Agendar
      </RouterLink>
    </li>
  </ul>
</template>

<style scoped>
.filters {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.tile {
  padding: 12px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--surface);
  color: var(--ink);
  font-weight: 500;
  text-align: left;
}

.tile:hover:not(:disabled) {
  background: var(--gray-soft);
}

.tile strong {
  display: block;
  font-size: 1.7rem;
  line-height: 1.2;
}

.tile-red strong {
  color: var(--red);
}

.tile-amber strong {
  color: var(--amber);
}

.tile.selected {
  border-color: var(--brand);
  box-shadow: inset 0 0 0 1px var(--brand);
}

.alert-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.alert {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 8px 24px;
  padding: 14px 16px;
  border-left-width: 5px;
}

.alert-red {
  border-left-color: var(--red);
}

.alert-amber {
  border-left-color: #d99a1c;
}

.alert-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
}

.alert-title h2 {
  font-size: 1.05rem;
}

.alert-hours {
  display: grid;
  text-align: right;
}

.alert-hours .muted {
  font-size: 0.85rem;
}

.alert-action {
  font-weight: 600;
}

.empty {
  padding: 24px;
  text-align: center;
  color: var(--muted);
}

@media (max-width: 720px) {
  .alert {
    grid-template-columns: minmax(0, 1fr);
  }

  .alert-hours {
    text-align: left;
  }
}
</style>
