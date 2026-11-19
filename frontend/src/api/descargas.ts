import { finalizarPeticion, iniciarPeticion } from './progreso'

export async function descargarArchivo(url: string, nombre: string): Promise<void> {
  const token = localStorage.getItem('magnetixdian_token')
  iniciarPeticion()
  try {
    const res = await fetch(url, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
    if (!res.ok) {
      throw new Error(`No se pudo descargar el archivo (${res.status}).`)
    }
    const blob = await res.blob()
    const objectUrl = URL.createObjectURL(blob)
    const a = window.document.createElement('a')
    a.href = objectUrl
    a.download = nombre
    a.click()
    URL.revokeObjectURL(objectUrl)
  } finally {
    finalizarPeticion()
  }
}
