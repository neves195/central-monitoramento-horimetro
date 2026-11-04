const hoursFormat = new Intl.NumberFormat('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })

// 12350.5 -> "12.350,5 h"
export function formatHours(value) {
  return value === null || value === undefined ? '-' : `${hoursFormat.format(value)} h`
}

// "2026-11-07" -> "07/11/2026". Monta pelo texto porque new Date('2026-11-07') lê como UTC
// e, no fuso do Brasil, a data apareceria um dia antes.
export function formatDate(value) {
  if (!value) {
    return '-'
  }
  const [year, month, day] = value.slice(0, 10).split('-')
  return `${day}/${month}/${year}`
}

// "2026-11-07T14:30:00" -> "07/11/2026 14:30"
export function formatDateTime(value) {
  return value ? `${formatDate(value)} ${value.slice(11, 16)}` : '-'
}

export function today() {
  const now = new Date()
  const month = String(now.getMonth() + 1).padStart(2, '0')
  const day = String(now.getDate()).padStart(2, '0')
  return `${now.getFullYear()}-${month}-${day}`
}
