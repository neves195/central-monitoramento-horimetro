import { auth, logout } from '../stores/auth'

// Erro da API no formato {status, error, messages[], timestamp}
export class ApiError extends Error {
  constructor(status, messages) {
    super(messages.join(' '))
    this.status = status
    this.messages = messages
  }
}

async function readBody(response) {
  const text = await response.text()
  try {
    return text ? JSON.parse(text) : null
  } catch {
    return null
  }
}

export async function api(path, { method = 'GET', body, authHeader } = {}) {
  // X-Requested-With faz o backend omitir o WWW-Authenticate no 401.
  // Sem isso, o navegador abriria a janela nativa de login por cima da tela.
  const headers = { 'X-Requested-With': 'XMLHttpRequest' }

  const credentials = authHeader ?? auth.header
  if (credentials) {
    headers.Authorization = credentials
  }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
  }

  let response
  try {
    response = await fetch(`/api${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, ['Não foi possível falar com o servidor.'])
  }

  const data = await readBody(response)

  if (!response.ok) {
    // Credenciais recusadas com alguém logado: volta para o login (o router reage ao logout)
    if (response.status === 401 && !authHeader) {
      logout()
    }
    // Proxy sem backend no ar responde 5xx sem o nosso formato de erro
    const fallback = response.status >= 500 ? 'Não foi possível falar com o servidor.' : data?.error
    throw new ApiError(response.status, data?.messages ?? [fallback ?? 'Erro inesperado.'])
  }

  return data
}
