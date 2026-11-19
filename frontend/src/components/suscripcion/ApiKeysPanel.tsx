import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Chip from '@mui/material/Chip'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import ContentCopyOutlinedIcon from '@mui/icons-material/ContentCopyOutlined'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import SectionCard from '../ui/SectionCard'
import EmptyState from '../ui/EmptyState'
import { crearApiKey, listarApiKeys, revocarApiKey, type ApiKey, type ApiKeyCreada } from '../../api/client'

function formatearFecha(valor: string | null) {
  if (!valor) return 'Nunca'
  const fecha = new Date(valor)
  return Number.isNaN(fecha.getTime()) ? valor : fecha.toLocaleString('es-CO')
}

export default function ApiKeysPanel({ empresaId }: { empresaId: number }) {
  const [claves, setClaves] = useState<ApiKey[]>([])
  const [cargando, setCargando] = useState(true)
  const [dialogo, setDialogo] = useState(false)
  const [nombre, setNombre] = useState('')
  const [creando, setCreando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [nueva, setNueva] = useState<ApiKeyCreada | null>(null)
  const [copiado, setCopiado] = useState(false)

  const cargar = useCallback(async () => {
    setCargando(true)
    try {
      setClaves(await listarApiKeys(empresaId))
    } catch {
      setError('No se pudieron cargar las claves de API.')
    } finally {
      setCargando(false)
    }
  }, [empresaId])

  useEffect(() => {
    void cargar()
  }, [cargar])

  const onCrear = async () => {
    setCreando(true)
    setError(null)
    try {
      const creada = await crearApiKey(empresaId, nombre.trim())
      setNueva(creada)
      setNombre('')
      setDialogo(false)
      await cargar()
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo crear la clave.')
    } finally {
      setCreando(false)
    }
  }

  const onRevocar = async (id: number) => {
    setError(null)
    try {
      await revocarApiKey(empresaId, id)
      await cargar()
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo revocar la clave.')
    }
  }

  const copiar = async () => {
    if (!nueva) return
    try {
      await navigator.clipboard.writeText(nueva.clave)
      setCopiado(true)
    } catch {
      setError('No se pudo copiar al portapapeles.')
    }
  }

  return (
    <SectionCard
      title="Claves de API"
      subtitle="Use una clave en el encabezado X-API-Key para integrar sus sistemas."
      actions={
        <Button variant="contained" onClick={() => setDialogo(true)}>
          Crear clave
        </Button>
      }
      padded={false}
    >
      <Box sx={{ px: { xs: 2, md: 3 }, py: 2, display: 'grid', gap: 2 }}>
        {error && <Alert severity="error">{error}</Alert>}

        {nueva && (
          <Alert severity="success" onClose={() => { setNueva(null); setCopiado(false) }}>
            <Stack spacing={1}>
              <Typography variant="body2">
                Copie ahora su clave. No volverá a mostrarse.
              </Typography>
              <Stack direction="row" spacing={1} alignItems="center">
                <Typography
                  component="code"
                  sx={{ fontFamily: 'monospace', wordBreak: 'break-all', flexGrow: 1 }}
                >
                  {nueva.clave}
                </Typography>
                <Tooltip title={copiado ? 'Copiado' : 'Copiar'}>
                  <IconButton onClick={copiar} aria-label="Copiar clave" size="small">
                    <ContentCopyOutlinedIcon fontSize="small" />
                  </IconButton>
                </Tooltip>
              </Stack>
            </Stack>
          </Alert>
        )}

        {!cargando && claves.length === 0 ? (
          <EmptyState
            title="Sin claves de API"
            description="Cree una clave para autenticar integraciones sin usuario ni contraseña."
          />
        ) : (
          <List disablePadding>
            {claves.map((clave) => (
              <ListItem
                key={clave.id}
                divider
                secondaryAction={
                  clave.activo ? (
                    <Tooltip title="Revocar clave">
                      <IconButton
                        edge="end"
                        aria-label={`Revocar clave ${clave.nombre}`}
                        onClick={() => onRevocar(clave.id)}
                      >
                        <DeleteOutlineIcon />
                      </IconButton>
                    </Tooltip>
                  ) : undefined
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="subtitle2">{clave.nombre}</Typography>
                      <Chip
                        label={clave.activo ? 'Activa' : 'Revocada'}
                        color={clave.activo ? 'success' : 'default'}
                        size="small"
                      />
                    </Stack>
                  }
                  secondary={
                    <>
                      <Typography component="span" variant="body2" sx={{ fontFamily: 'monospace' }}>
                        mx_{clave.prefijo}…
                      </Typography>
                      <Typography component="span" variant="body2" color="text.secondary">
                        {' · Último uso: '}
                        {formatearFecha(clave.ultimoUso)}
                      </Typography>
                    </>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </Box>

      <Dialog open={dialogo} onClose={() => setDialogo(false)} fullWidth maxWidth="xs">
        <DialogTitle>Nueva clave de API</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            fullWidth
            label="Nombre"
            helperText="Un nombre que identifique el uso de la clave."
            value={nombre}
            onChange={(e) => setNombre(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogo(false)}>Cancelar</Button>
          <Button variant="contained" onClick={onCrear} disabled={!nombre.trim()} loading={creando}>
            Crear
          </Button>
        </DialogActions>
      </Dialog>
    </SectionCard>
  )
}
