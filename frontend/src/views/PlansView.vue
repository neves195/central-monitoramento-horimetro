<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api/client'
import EquipmentHeader from '../components/EquipmentHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { formatHours } from '../format'
import { auth } from '../stores/auth'

const props = defineProps({ id: { type: String, required: true } })

const equipment = ref(null)
const plans = ref([])
const loading = ref(true)
const loadError = ref('')

const emptyForm = { name: '', intervalHours: '', warningHours: '', lastMaintenanceHours: '' }
const form = reactive({ ...emptyForm })
const formError = ref('')
const sending = ref(false)

async function load() {
  try {
    const [equipmentData, plansData] = await Promise.all([
      api(`/equipments/${props.id}`),
      api(`/equipments/${props.id}/plans`),
    ])
    equipment.value = equipmentData
    plans.value = plansData
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

async function submit() {
  formError.value = ''
  sending.value = true
  try {
    await api(`/equipments/${props.id}/plans`, {
      method: 'POST',
      // Última manutenção vazia: o backend começa a contar do horímetro atual
      body: { ...form, lastMaintenanceHours: form.lastMaintenanceHours === '' ? null : form.lastMaintenanceHours },
    })
    Object.assign(form, emptyForm)
    await load()
  } catch (e) {
    formError.value = e.message
  } finally {
    sending.value = false
  }
}

onMounted(load)
</script>

<template>
  <EquipmentHeader :id="id" :equipment="equipment" />

  <p v-if="loadError" class="error" role="alert">{{ loadError }}</p>

  <form v-if="auth.isAdmin && !loadError" class="card form" @submit.prevent="submit">
    <h2>Novo plano</h2>
    <div class="form-grid">
      <label>
        Nome
        <input v-model="form.name" maxlength="120" required />
      </label>
      <label>
        Intervalo (h)
        <input v-model="form.intervalHours" type="number" min="0.1" step="0.1" required />
      </label>
      <label>
        Avisar antes de (h)
        <input v-model="form.warningHours" type="number" min="0" step="0.1" required />
      </label>
      <label>
        Última manutenção (h, opcional)
        <input v-model="form.lastMaintenanceHours" type="number" min="0" step="0.1" />
      </label>
    </div>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div>
      <button type="submit" :disabled="sending">Criar plano</button>
    </div>
  </form>

  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!loadError && plans.length === 0" class="card table-empty">Nenhum plano ativo.</p>

  <div v-else-if="!loadError" class="card table-wrap">
    <table class="stack">
      <thead>
        <tr>
          <th>Plano</th>
          <th>Situação</th>
          <th class="number">Intervalo</th>
          <th class="number">Aviso</th>
          <th class="number">Última</th>
          <th class="number">Vence em</th>
          <th class="number">Restante</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="plan in plans" :key="plan.id">
          <td data-label="Plano"><strong>{{ plan.name }}</strong></td>
          <td data-label="Situação"><StatusBadge :status="plan.status" /></td>
          <td data-label="Intervalo" class="number">{{ formatHours(plan.intervalHours) }}</td>
          <td data-label="Aviso" class="number">{{ formatHours(plan.warningHours) }}</td>
          <td data-label="Última" class="number">{{ formatHours(plan.lastMaintenanceHours) }}</td>
          <td data-label="Vence em" class="number">{{ formatHours(plan.nextDueHours) }}</td>
          <td data-label="Restante" class="number">{{ formatHours(plan.remainingHours) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
