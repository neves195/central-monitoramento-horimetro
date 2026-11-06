/**
 * Ponto de entrada do frontend: cria o app Vue, registra o roteador e carrega os estilos globais.
 */
import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router'
import './styles.css'

createApp(App).use(router).mount('#app')
