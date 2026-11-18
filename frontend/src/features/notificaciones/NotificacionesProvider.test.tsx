import { act, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, expect, it } from 'vitest'
import { EVENTO_ERROR_API } from '../../api/client'
import { NotificacionesProvider } from './NotificacionesProvider'
import { useNotificaciones } from './contexto'

function Boton() {
  const { notificar } = useNotificaciones()
  return <button onClick={() => notificar('Guardado correctamente', 'success')}>Avisar</button>
}

describe('NotificacionesProvider', () => {
  it('muestra el mensaje al invocar notificar', async () => {
    const user = userEvent.setup()
    render(
      <NotificacionesProvider>
        <Boton />
      </NotificacionesProvider>,
    )
    await user.click(screen.getByText('Avisar'))
    expect(await screen.findByText('Guardado correctamente')).toBeInTheDocument()
  })

  it('muestra los errores globales emitidos por la API', async () => {
    render(
      <NotificacionesProvider>
        <div>Aplicación</div>
      </NotificacionesProvider>,
    )
    act(() => {
      window.dispatchEvent(new CustomEvent(EVENTO_ERROR_API, { detail: 'Fallo de red' }))
    })
    expect(await screen.findByText('Fallo de red')).toBeInTheDocument()
  })
})
