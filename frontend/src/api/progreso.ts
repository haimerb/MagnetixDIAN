let pendientes = 0
const oyentes = new Set<() => void>()

function notificar() {
  for (const oyente of oyentes) oyente()
}

export function iniciarPeticion() {
  pendientes += 1
  notificar()
}

export function finalizarPeticion() {
  pendientes = Math.max(0, pendientes - 1)
  notificar()
}

export function peticionesActivas() {
  return pendientes
}

export function suscribirProgreso(oyente: () => void) {
  oyentes.add(oyente)
  return () => {
    oyentes.delete(oyente)
  }
}
