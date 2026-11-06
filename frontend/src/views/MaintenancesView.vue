<!--
  Manutenções de um equipamento: histórico, agendamento (com plano ou avulsa) e, para ADMIN, conclusão e cancelamento.
-->
<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { api } from '../api/client'
import EquipmentHeader from '../components/EquipmentHeader.vue'
import StatusBadge from '../components/StatusBadge.vue'
import { formatDate, formatHours, today } from '../format'
import { auth } from '../stores/auth'

const props = defineProps({ id: { type: String, required: true } })
const route = useRoute()

const equipment = ref(null)
const plans = ref([])
const maintenances = ref([])
const loading = ref(true)
const loadError = ref('')

// Vindo do painel de alertas, o plano já chega selecionado
const form = reactive({
  planId: route.query.planId ?? '',
  scheduledDate: today(),
  description: '',
  responsible: '',
})
const formError = ref('')
const sending = ref(false)

// Só uma manutenção por vez fica com o formulário de conclusão aberto
const completing = ref(null)
const completion = reactive({ performedAt: '', hoursAtMaintenance: '', responsible: '' })
const actionError = ref('')

const planNames = computed(() => new Map(plans.value.map((plan) => [plan.id, plan.name])))

// Nome do plano da manutenção, ou "Avulsa, sem plano"
function planLabel(maintenance) {
  if (maintenance.planId === null) {
    return 'Avulsa, sem plano'
  }
  return planNames.value.get(maintenance.planId) ?? `Plano ${maintenance.planId}`
}

// Busca equipamento, planos e manutenções ao mesmo tempo
async function load() {
  try {
    const [equipmentData, plansData, maintenancesData] = await Promise.all([
      api(`/equipments/${props.id}`),
      api(`/equipments/${props.id}/plans`),
      api(`/equipments/${props.id}/maintenances`),
    ])
    equipment.value = equipmentData
    plans.value = plansData
    maintenances.value = maintenancesData
  } catch (e) {
    loadError.value = e.message
  } finally {
    loading.value = false
  }
}

// Agenda uma manutenção e recarrega a lista
async function schedule() {
  formError.value = ''
  sending.value = true
  try {
    await api(`/equipments/${props.id}/maintenances`, {
      method: 'POST',
      body: {
        planId: form.planId || null,
        scheduledDate: form.scheduledDate,
        description: form.description,
        responsible: form.responsible || null,
      },
    })
    form.planId = ''
    form.description = ''
    form.responsible = ''
    await load()
  } catch (e) {
    formError.value = e.message
  } finally {
    sending.value = false
  }
}

// Abre o formulário de conclusão da manutenção, já com a data de hoje
function openCompletion(maintenance) {
  actionError.value = ''
  completing.value = maintenance.id
  completion.performedAt = today()
  completion.hoursAtMaintenance = ''
  completion.responsible = maintenance.responsible ?? ''
}

// Conclui a manutenção; se houver plano, o backend zera a contagem dele
async function complete(maintenance) {
  actionError.value = ''
  sending.value = true
  try {
    await api(`/maintenances/${maintenance.id}/complete`, {
      method: 'POST',
      // Horas vazias: o backend usa o horímetro atual
      body: {
        performedAt: completion.performedAt || null,
        hoursAtMaintenance: completion.hoursAtMaintenance === '' ? null : completion.hoursAtMaintenance,
        responsible: completion.responsible || null,
      },
    })
    completing.value = null
    await load()
  } catch (e) {
    actionError.value = e.message
  } finally {
    sending.value = false
  }
}

// Cancela a manutenção depois de pedir confirmação
async function cancel(maintenance) {
  if (!window.confirm(`Cancelar a manutenção "${maintenance.description}"?`)) {
    return
  }
  actionError.value = ''
  sending.value = true
  try {
    await api(`/maintenances/${maintenance.id}/cancel`, { method: 'POST' })
    await load()
  } catch (e) {
    actionError.value = e.message
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

  <form v-if="auth.isAdmin && !loadError" class="card form" @submit.prevent="schedule">
    <h2>Agendar manutenção</h2>
    <div class="form-grid">
      <label>
        Plano
        <select v-model="form.planId">
          <option value="">Avulsa, sem plano</option>
          <option v-for="plan in plans" :key="plan.id" :value="String(plan.id)">{{ plan.name }}</option>
        </select>
      </label>
      <label>
        Data agendada
        <input v-model="form.scheduledDate" type="date" required />
      </label>
      <label>
        Descrição
        <input v-model="form.description" maxlength="500" required />
      </label>
      <label>
        Responsável (opcional)
        <input v-model="form.responsible" maxlength="100" />
      </label>
    </div>
    <p v-if="formError" class="error" role="alert">{{ formError }}</p>
    <div>
      <button type="submit" :disabled="sending">Agendar</button>
    </div>
  </form>

  <p v-if="actionError" class="error" role="alert">{{ actionError }}</p>
  <p v-if="loading" class="muted">Carregando...</p>
  <p v-else-if="!loadError && maintenances.length === 0" class="card table-empty">Nenhuma manutenção registrada.</p>

  <ul v-else-if="!loadError" class="maintenance-list">
    <li v-for="maintenance in maintenances" :key="maintenance.id" class="card maintenance">
      <div class="maintenance-title">
        <StatusBadge :status="maintenance.status" />
        <h2>{{ maintenance.description }}</h2>
      </div>

      <dl>
        <div>
          <dt>Plano</dt>
          <dd>{{ planLabel(maintenance) }}</dd>
        </div>
        <div>
          <dt>Agendada para</dt>
          <dd>{{ formatDate(maintenance.scheduledDate) }}</dd>
        </div>
        <div v-if="maintenance.status === 'COMPLETED'">
          <dt>Realizada em</dt>
          <dd>{{ formatDate(maintenance.performedAt) }}</dd>
        </div>
        <div v-if="maintenance.status === 'COMPLETED'">
          <dt>Horímetro</dt>
          <dd>{{ formatHours(maintenance.hoursAtMaintenance) }}</dd>
        </div>
        <div>
          <dt>Responsável</dt>
          <dd>{{ maintenance.responsible ?? '-' }}</dd>
        </div>
      </dl>

      <!-- Só agendada pode mudar de estado -->
      <template v-if="auth.isAdmin && maintenance.status === 'SCHEDULED'">
        <form v-if="completing === maintenance.id" class="completion" @submit.prevent="complete(maintenance)">
          <div class="form-grid">
            <label>
              Data de realização
              <input v-model="completion.performedAt" type="date" required />
            </label>
            <label>
              Horímetro (vazio = atual)
              <input
                v-model="completion.hoursAtMaintenance"
                type="number"
                min="0"
                step="0.1"
                :placeholder="equipment?.currentHours"
              />
            </label>
            <label>
              Responsável
              <input v-model="completion.responsible" maxlength="100" />
            </label>
          </div>
          <div class="actions">
            <button type="submit" :disabled="sending">Confirmar conclusão</button>
            <button type="button" class="button-secondary" @click="completing = null">Voltar</button>
          </div>
        </form>

        <div v-else class="actions">
          <button type="button" @click="openCompletion(maintenance)">Concluir</button>
          <button type="button" class="button-secondary" :disabled="sending" @click="cancel(maintenance)">
            Cancelar
          </button>
        </div>
      </template>
    </li>
  </ul>
</template>

<style scoped>
.maintenance-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: grid;
  gap: 10px;
}

.maintenance {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
}

.maintenance-title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 4px 10px;
}

.maintenance-title h2 {
  font-size: 1.05rem;
}

dl {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 8px 20px;
  margin: 0;
}

dt {
  color: var(--muted);
  font-size: 0.8rem;
}

dd {
  margin: 0;
}

.completion {
  display: grid;
  gap: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
</style>
