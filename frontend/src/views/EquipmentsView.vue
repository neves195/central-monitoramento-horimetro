<!--
  Lista de equipamentos ativos, com o horímetro atual e o acesso às leituras, planos e manutenções de cada um.
  Para ADMIN, inclui o formulário de cadastro de novo equipamento.
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { api } from '../api/client'
import { formatHours } from '../format'
import { auth } from '../stores/auth'

const equipments = ref([])
const loading = ref(true)
const loadError = ref('')

const emptyForm = { code: '', name: '', category: '' }
const form = reactive({ ...emptyForm })
// Lista, e não texto único: o 400 de validação traz uma mensagem por campo
const formErrors = ref([])
const sending = ref(false)

// Busca os equipamentos ativos
async function load() {
  loadError.value = ''
  try {
    equipments.value = await api('/equipments')
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

// Cadastra o equipamento (só ADMIN) e recarrega a lista; erros de validação vêm um por campo
async function submit() {
  formErrors.value = []
  sending.value = true
  try {
    await api('/equipments', {
      method: 'POST',
      // Categoria vazia vai como null: é um campo opcional, e a lista mostra "-"
      body: { ...form, category: form.category.trim() || null },
    })
    Object.assign(form, emptyForm)
    await load()
  } catch (e) {
    formErrors.value = e.messages
  } finally {
    sending.value = false
  }
}

// Carrega a lista ao abrir a tela
onMounted(load)
</script>

<template>
  <div class="page-title">
    <h1>Equipamentos</h1>
  </div>

  <p v-if="loadError" class="error" role="alert">{{ loadError }}</p>

  <!-- VIEWER não vê o formulário; a proteção real é o 403 do backend -->
  <form v-if="auth.isAdmin && !loadError" class="card form" @submit.prevent="submit">
    <h2>Novo equipamento</h2>
    <div class="form-grid">
      <label>
        Código
        <input v-model="form.code" maxlength="30" required />
      </label>
      <label>
        Nome
        <input v-model="form.name" maxlength="120" required />
      </label>
      <label>
        Categoria (opcional)
        <input v-model="form.category" maxlength="60" />
      </label>
    </div>
    <div v-if="formErrors.length" class="error" role="alert">
      <p v-for="message in formErrors" :key="message">{{ message }}</p>
    </div>
    <div>
      <button type="submit" :disabled="sending">Criar equipamento</button>
    </div>
  </form>

  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!loadError && equipments.length === 0" class="card table-empty">Nenhum equipamento ativo.</p>

  <div v-else-if="!loadError" class="card table-wrap">
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
