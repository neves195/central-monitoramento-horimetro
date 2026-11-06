/**
 * Estado de autenticação: usuário, papéis e o header Basic, guardados só em memória, com as funções login e logout.
 */
import { reactive } from 'vue'
import { api } from '../api/client'

// As credenciais ficam SÓ neste objeto, em memória: nunca em localStorage ou cookie.
// Fechar ou recarregar a página apaga tudo e pede login de novo. É uma escolha de segurança,
// porque no HTTP Basic o header carrega a própria senha (Base64 não é criptografia).
export const auth = reactive({
  username: null,
  roles: [],
  header: null,

  // Há alguém logado
  get isLoggedIn() {
    return this.username !== null
  },

  // Só ADMIN vê os formulários de escrita (o backend também exige o papel)
  get isAdmin() {
    return this.roles.includes('ADMIN')
  },
})

// btoa só aceita Latin-1, então a senha passa antes por UTF-8 (o Spring lê o Basic em UTF-8)
function basicHeader(username, password) {
  const bytes = new TextEncoder().encode(`${username}:${password}`)
  return `Basic ${btoa(String.fromCharCode(...bytes))}`
}

// Só guarda as credenciais depois que o backend aceitar
export async function login(username, password) {
  const header = basicHeader(username, password)
  const me = await api('/me', { authHeader: header })

  auth.header = header
  auth.username = me.username
  auth.roles = me.roles
}

// Apaga as credenciais da memória; o router reage e leva para a tela de login
export function logout() {
  auth.header = null
  auth.username = null
  auth.roles = []
}
