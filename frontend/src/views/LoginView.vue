<!--
  Tela de login: pede usuário e senha e valida no backend (GET /api/me) antes de entrar. Depois do login volta para a página que a pessoa queria abrir.
-->
<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { login } from '../stores/auth'

const route = useRoute()
const router = useRouter()

const username = ref('')
const password = ref('')
const error = ref('')
const sending = ref(false)

// Tenta entrar; 401 vira mensagem de credenciais inválidas e qualquer outro erro mostra o texto da API
async function submit() {
  error.value = ''
  sending.value = true
  try {
    await login(username.value.trim(), password.value)
    router.push(route.query.redirect ?? '/')
  } catch (e) {
    error.value = e.status === 401 ? 'Usuário ou senha inválidos.' : e.message
  } finally {
    // A senha não fica na tela depois da tentativa
    password.value = ''
    sending.value = false
  }
}
</script>

<template>
  <section class="login">
    <form class="card login-card" @submit.prevent="submit">
      <h1>Central de Manutenção</h1>
      <p class="muted">Monitoramento de manutenção preventiva por horímetro</p>

      <label>
        Usuário
        <input v-model="username" autocomplete="username" required autofocus />
      </label>
      <label>
        Senha
        <input v-model="password" type="password" autocomplete="current-password" required />
      </label>

      <p v-if="error" class="error" role="alert">{{ error }}</p>

      <button type="submit" :disabled="sending">{{ sending ? 'Entrando...' : 'Entrar' }}</button>
    </form>
  </section>
</template>

<style scoped>
.login {
  min-height: 100dvh;
  display: grid;
  place-items: center;
  padding: 16px;
}

.login-card {
  width: 100%;
  max-width: 380px;
  display: grid;
  gap: 16px;
  padding: 28px;
}

h1 {
  font-size: 1.4rem;
}

p.muted {
  margin-top: -10px;
}
</style>
