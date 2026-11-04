<script setup>
import { auth, logout } from './stores/auth'
</script>

<template>
  <header v-if="auth.isLoggedIn" class="topbar">
    <div class="topbar-inner">
      <span class="brand">Central de Manutenção</span>
      <nav class="mainnav" aria-label="Principal">
        <RouterLink to="/">Painel</RouterLink>
      </nav>
      <div class="session">
        <span>{{ auth.username }}</span>
        <span class="role">{{ auth.isAdmin ? 'Administrador' : 'Consulta' }}</span>
        <button type="button" class="button-quiet" @click="logout">Sair</button>
      </div>
    </div>
  </header>

  <main :class="auth.isLoggedIn ? 'page' : ''">
    <RouterView />
  </main>
</template>
