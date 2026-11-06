<!--
  Selo colorido para o status de um plano (OVERDUE, UPCOMING, ON_TIME) ou de uma manutenção (SCHEDULED, COMPLETED, CANCELED).
-->
<script setup>
import { computed } from 'vue'

const props = defineProps({ status: { type: String, required: true } })

// Status de plano e de manutenção usam o mesmo selo. O texto acompanha a cor: a situação
// não pode depender só de enxergar a cor.
const styles = {
  OVERDUE: { label: 'Vencido', tone: 'red' },
  UPCOMING: { label: 'Próximo', tone: 'amber' },
  ON_TIME: { label: 'Em dia', tone: 'green' },
  SCHEDULED: { label: 'Agendada', tone: 'amber' },
  COMPLETED: { label: 'Concluída', tone: 'green' },
  CANCELED: { label: 'Cancelada', tone: 'gray' },
}

const style = computed(() => styles[props.status] ?? { label: props.status, tone: 'gray' })
</script>

<template>
  <span class="badge" :class="style.tone">{{ style.label }}</span>
</template>

<style scoped>
.badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.8rem;
  font-weight: 700;
  white-space: nowrap;
}

.red {
  color: var(--red);
  background: var(--red-soft);
}

.amber {
  color: var(--amber);
  background: var(--amber-soft);
}

.green {
  color: var(--green);
  background: var(--green-soft);
}

.gray {
  color: var(--muted);
  background: var(--gray-soft);
}
</style>
