<!--
  Leituras de horímetro de um equipamento: histórico e, para ADMIN, o formulário para lançar uma nova leitura.
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api/client'
import EquipmentHeader from '../components/EquipmentHeader.vue'
import { formatDateTime, formatHours } from '../format'
import { auth } from '../stores/auth'

const props = defineProps({ id: { type: String, required: true } })

const equipment = ref(null)
const readings = ref([])
const loading = ref(true)
const loadError = ref('')

const form = reactive({ hours: '', readAt: '', registeredBy: auth.username })
const formError = ref('')
const sending = ref(false)

// Busca o equipamento e o histórico de leituras ao mesmo tempo
async function load() {
  try {
    const [equipmentData, readingsData] = await Promise.all([
      api(`/equipments/${props.id}`),
      api(`/equipments/${props.id}/readings`),
    ])
    equipment.value = equipmentData
    readings.value = readingsData
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

// Lança a leitura e recarrega a tela (o horímetro do equipamento também muda)
async function submit() {
  formError.value = ''
  sending.value = true
  try {
    await api(`/equipments/${props.id}/readings`, {
      method: 'POST',
      // Data vazia: o backend assume o momento do registro
      body: { hours: form.hours, readAt: form.readAt || null, registeredBy: form.registeredBy },
    })
    form.hours = ''
    form.readAt = ''
    await load()
  } catch (e) {
    formError.value = e.message
  } finally {
    sending.value = false
  }
}

// Carrega os dados ao abrir a tela
onMounted(load)
</script>

<template>
  <EquipmentHeader :id="id" :equipment="equipment" />

  <p v-if="loadError" class="error" role="alert">{{ loadError }}</p>

  <!-- VIEWER não vê o formulário; a proteção real é o 403 do backend -->
  <form v-if="auth.isAdmin && !loadError" class="card form" @submit.prevent="submit">
    <h2>Lançar leitura</h2>
    <div class="form-grid">
      <label>
        Horímetro (h)
        <input v-model="form.hours" type="number" min="0" step="0.1" required />
      </label>
      <label>
        Data da leitura (opcional)
        <input v-model="form.readAt" type="datetime-local" />
      </label>
      <label>
        Registrado por
        <input v-model="form.registeredBy" maxlength="50" required />
      </label>
    </div>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div>
      <button type="submit" :disabled="sending">Lançar leitura</button>
    </div>
  </form>

  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!loadError && readings.length === 0" class="card table-empty">Nenhuma leitura registrada.</p>

  <div v-else-if="!loadError" class="card table-wrap">
    <table class="stack">
      <thead>
        <tr>
          <th>Data da leitura</th>
          <th>Registrado por</th>
          <th class="number">Horímetro</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="reading in readings" :key="reading.id">
          <td data-label="Data">{{ formatDateTime(reading.readAt) }}</td>
          <td data-label="Registrado por">{{ reading.registeredBy }}</td>
          <td data-label="Horímetro" class="number">{{ formatHours(reading.hours) }}</td>
        </tr>
      </tbody>
    </table>
  </div>
</template>
