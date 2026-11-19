import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import { cambiarPassword } from '../api/client'

export default function CambiarPasswordPage() {
  const [actual, setActual] = useState('')
  const [nueva, setNueva] = useState('')
  const [confirmacion, setConfirmacion] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [ok, setOk] = useState<string | null>(null)
  const [cargando, setCargando] = useState(false)

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)
    setOk(null)
    if (nueva !== confirmacion) {
      setError('Las contraseñas nuevas no coinciden.')
      return
    }
    setCargando(true)
    try {
      await cambiarPassword(actual, nueva)
      setOk('Contraseña actualizada. Por seguridad, sus otras sesiones fueron cerradas.')
      setActual('')
      setNueva('')
      setConfirmacion('')
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo cambiar la contraseña.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3, maxWidth: 560 }}>
      <PageHeader
        overline="Seguridad"
        title="Cambiar contraseña"
        subtitle="Use al menos 8 caracteres, combinando letras y números."
      />
      <SectionCard title="Contraseña actual">
        <Box component="form" noValidate onSubmit={onSubmit} sx={{ display: 'grid', gap: 2 }}>
          <TextField
            label="Contraseña actual"
            type="password"
            autoComplete="current-password"
            value={actual}
            onChange={(e) => setActual(e.target.value)}
            required
          />
          <TextField
            label="Nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={nueva}
            onChange={(e) => setNueva(e.target.value)}
            required
          />
          <TextField
            label="Confirmar nueva contraseña"
            type="password"
            autoComplete="new-password"
            value={confirmacion}
            onChange={(e) => setConfirmacion(e.target.value)}
            required
          />
          {error && <Alert severity="error">{error}</Alert>}
          {ok && <Alert severity="success">{ok}</Alert>}
          <Button
            type="submit"
            variant="contained"
            disabled={cargando || !actual || nueva.length < 8}
            loading={cargando}
            sx={{ justifySelf: 'start' }}
          >
            Guardar contraseña
          </Button>
        </Box>
      </SectionCard>
    </Box>
  )
}
