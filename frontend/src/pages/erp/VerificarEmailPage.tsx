import { useEffect, useState } from 'react'
import { Link as RouterLink, useSearchParams } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { reenviarVerificacionEmail, tokenDeVerificacion, verificarEmail } from '../../api/erp/cuenta'
import type { VerificacionEmail } from '../../api/erp/cuenta'
import Logo from '../../components/ui/Logo'
import LoadingState from '../../components/ui/LoadingState'

export default function VerificarEmailPage() {
  const [params] = useSearchParams()
  const token = tokenDeVerificacion(params.get('token'))
  const [email, setEmail] = useState(params.get('email') ?? '')
  const [resultado, setResultado] = useState<VerificacionEmail | null>(null)
  const [cargando, setCargando] = useState(Boolean(token))
  const [enviando, setEnviando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (!token) return
    let activo = true
    verificarEmail(token)
      .then((datos) => {
        if (activo) setResultado(datos)
      })
      .catch(() => {
        if (activo) setError('El enlace de verificación no es válido o ya fue usado.')
      })
      .finally(() => {
        if (activo) setCargando(false)
      })
    return () => {
      activo = false
    }
  }, [token])

  const reenviar = async () => {
    if (!email.trim()) return
    setEnviando(true)
    setError(null)
    try {
      await reenviarVerificacionEmail(email.trim())
      setResultado({
        verificada: false,
        email: email.trim(),
        mensaje: 'Envíe el enlace desde su correo para completar la verificación.',
      })
    } catch {
      setError('No se pudo reenviar el correo de verificación.')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <Box component="main" sx={{ minHeight: '100vh', display: 'grid', placeItems: 'center', p: 3, bgcolor: 'background.default' }}>
      <Paper elevation={0} sx={{ width: '100%', maxWidth: 520, p: { xs: 3, md: 4 }, border: '1px solid', borderColor: 'divider' }}>
        <Box sx={{ mb: 3 }}>
          <Logo size={34} />
        </Box>
        <Typography variant="h5" component="h1" gutterBottom>
          Verificación de correo
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Confirme su dirección para habilitar la emisión de facturas y el envío al adquirente.
        </Typography>

        {cargando ? (
          <LoadingState label="Verificando correo…" minHeight="20vh" />
        ) : (
          <Stack spacing={2}>
            {resultado && (
              <Alert severity={resultado.verificada ? 'success' : 'info'}>{resultado.mensaje}</Alert>
            )}
            {error && <Alert severity="error">{error}</Alert>}
            {!token && (
              <>
                <TextField
                  label="Correo electrónico"
                  type="email"
                  value={email}
                  onChange={(evento) => setEmail(evento.target.value)}
                  fullWidth
                />
                <Button variant="contained" loading={enviando} disabled={!email.trim()} onClick={() => void reenviar()}>
                  Reenviar verificación
                </Button>
              </>
            )}
            {resultado?.verificada && (
              <Button component={RouterLink} to="/app/habilitacion-dian" variant="contained">
                Continuar a habilitación DIAN
              </Button>
            )}
            <Button component={RouterLink} to="/login" variant="text">
              Ir al inicio de sesión
            </Button>
          </Stack>
        )}
      </Paper>
    </Box>
  )
}
