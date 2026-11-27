import { fireEvent, render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes, useNavigate } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import AppLayout from './AppLayout'

vi.mock('../../features/auth/useAuth', () => ({
  useAuth: () => ({
    sesion: { username: 'admin', roles: ['ROLE_ADMIN'] },
    logout: vi.fn(),
  }),
}))

vi.mock('../../theme/useThemeMode', () => ({
  useThemeMode: () => ({ mode: 'light', alternar: vi.fn(), branding: undefined }),
}))

function Navegador() {
  const navigate = useNavigate()
  return <button onClick={() => navigate('/app/facturacion')}>Ir a facturación</button>
}

function renderLayout() {
  return render(
    <MemoryRouter initialEntries={['/app/erp/ventas']}>
      <Routes>
        <Route
          path="/app/*"
          element={
            <>
              <AppLayout />
              <Navegador />
            </>
          }
        />
      </Routes>
    </MemoryRouter>,
  )
}

describe('AppLayout', () => {
  it('alterna el submenú ERP al pulsar ERP dos veces', () => {
    renderLayout()
    const botonErp = screen.getByRole('button', { name: 'ERP' })

    fireEvent.click(botonErp)
    expect(screen.getByRole('menu')).toBeInTheDocument()

    fireEvent.click(botonErp)
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('cierra el submenú ERP al cambiar de ruta', () => {
    renderLayout()
    const botonErp = screen.getByRole('button', { name: 'ERP' })

    fireEvent.click(botonErp)
    expect(screen.getByRole('menu')).toBeInTheDocument()

    fireEvent.click(screen.getByText('Ir a facturación'))
    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
  })

  it('cierra el submenú al elegir un dominio sin bloquear el documento', () => {
    renderLayout()

    fireEvent.click(screen.getByRole('button', { name: 'ERP' }))
    fireEvent.click(screen.getByRole('menuitem', { name: 'Compras' }))

    expect(screen.queryByRole('menu')).not.toBeInTheDocument()
    expect(document.body.style.overflow).not.toBe('hidden')
  })
})
