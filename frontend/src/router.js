import { watch } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { auth } from './stores/auth'
import AlertsView from './views/AlertsView.vue'
import EquipmentsView from './views/EquipmentsView.vue'
import LoginView from './views/LoginView.vue'
import PlansView from './views/PlansView.vue'
import ReadingsView from './views/ReadingsView.vue'

const routes = [
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/', name: 'alerts', component: AlertsView },
  { path: '/equipamentos', name: 'equipments', component: EquipmentsView },
  { path: '/equipamentos/:id/leituras', name: 'readings', component: ReadingsView, props: true },
  { path: '/equipamentos/:id/planos', name: 'plans', component: PlansView, props: true },
  { path: '/equipamentos/:id/manutencoes', name: 'maintenances', component: { render: () => null } },
  { path: '/:pathMatch(.*)*', redirect: '/' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Sem login, toda rota cai na tela de login, lembrando para onde a pessoa queria ir
router.beforeEach((to) => {
  if (!to.meta.public && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.name === 'login' && auth.isLoggedIn) {
    return { path: '/' }
  }
})

// Saiu da sessão (botão Sair ou 401 no meio do uso): volta para o login
watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (!loggedIn) {
      router.push({ name: 'login' })
    }
  },
)
