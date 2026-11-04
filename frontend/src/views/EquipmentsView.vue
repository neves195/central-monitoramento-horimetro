<script setup>
import { onMounted, ref } from 'vue'
import { api } from '../api/client'
import { formatHours } from '../format'

const equipments = ref([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  try {
    equipments.value = await api('/equipments')
  } catch (e) {
    error.value = e.message
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page-title">
    <h1>Equipamentos</h1>
  </div>

  <p v-if="error" class="error" role="alert">{{ error }}</p>
  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!error && equipments.length === 0" class="card table-empty">Nenhum equipamento ativo.</p>

  <div v-else-if="!error" class="card table-wrap">
    <table class="stack">
      <thead>
        <tr>
          <th>Código</th>
          <th>Nome</th>
          <th>Categoria</th>
          <th class="number">Horímetro atual</th>
          <th>Consultar</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="equipment in equipments" :key="equipment.id">
          <td data-label="Código"><strong>{{ equipment.code }}</strong></td>
          <td data-label="Nome">{{ equipment.name }}</td>
          <td data-label="Categoria">{{ equipment.category ?? '-' }}</td>
          <td data-label="Horímetro" class="number">{{ formatHours(equipment.currentHours) }}</td>
          <td data-label="Consultar" class="links">
            <RouterLink :to="{ name: 'readings', params: { id: equipment.id } }">Leituras</RouterLink>
            <RouterLink :to="{ name: 'plans', params: { id: equipment.id } }">Planos</RouterLink>
            <RouterLink :to="{ name: 'maintenances', params: { id: equipment.id } }">Manutenções</RouterLink>
          </td>
        </tr>
      </tbody>
    </table>
  </div>
</template>

<style scoped>
.links {
  display: flex;
  flex-wrap: wrap;
  gap: 4px 12px;
}
</style>
