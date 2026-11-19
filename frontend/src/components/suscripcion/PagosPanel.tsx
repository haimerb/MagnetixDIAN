import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import IconButton from '@mui/material/IconButton'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import RefreshOutlinedIcon from '@mui/icons-material/RefreshOutlined'
import SectionCard from '../ui/SectionCard'
import EmptyState from '../ui/EmptyState'
import EstadoChip from '../ui/EstadoChip'
import { listarPagos, obtenerPago, type Pago } from '../../api/client'

function formatearMonto(pago: Pago) {
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: pago.moneda || 'COP',
    maximumFractionDigits: 0,
  }).format(pago.monto)
}

export default function PagosPanel({ empresaId }: { empresaId: number }) {
  const [pagos, setPagos] = useState<Pago[]>([])
  const [error, setError] = useState<string | null>(null)
  const [actualizando, setActualizando] = useState<string | null>(null)

  const actualizar = async (referencia: string) => {
    setActualizando(referencia)
    try {
      const actualizado = await obtenerPago(empresaId, referencia)
      setPagos((previos) => previos.map((pago) => (pago.id === actualizado.id ? actualizado : pago)))
    } catch {
      setError('No se pudo actualizar el estado del pago.')
    } finally {
      setActualizando(null)
    }
  }

  const cargar = useCallback(async () => {
    try {
      setPagos(await listarPagos(empresaId))
    } catch {
      setError('No se pudo cargar el historial de pagos.')
    }
  }, [empresaId])

  useEffect(() => {
    void cargar()
  }, [cargar])

  return (
    <SectionCard title="Historial de pagos" subtitle="Intentos de pago de su suscripción." padded={false}>
      <Box sx={{ px: { xs: 2, md: 3 }, py: 2 }}>
        {error && <Alert severity="error">{error}</Alert>}
        {pagos.length === 0 ? (
          <EmptyState title="Sin pagos" description="Aún no se han registrado pagos de suscripción." />
        ) : (
          <List disablePadding>
            {pagos.map((pago) => (
              <ListItem
                key={pago.id}
                divider
                secondaryAction={
                  <Stack direction="row" spacing={1} alignItems="center">
                    <Tooltip title="Actualizar estado">
                      <span>
                        <IconButton
                          size="small"
                          aria-label={`Actualizar estado de ${pago.referencia}`}
                          disabled={actualizando === pago.referencia}
                          onClick={() => void actualizar(pago.referencia)}
                        >
                          <RefreshOutlinedIcon fontSize="small" />
                        </IconButton>
                      </span>
                    </Tooltip>
                    <EstadoChip estado={pago.estado} />
                  </Stack>
                }
              >
                <ListItemText
                  primary={
                    <Stack direction="row" spacing={1} alignItems="center">
                      <Typography variant="subtitle2">Plan {pago.planCodigo}</Typography>
                      <Typography variant="body2" color="text.secondary">
                        {formatearMonto(pago)}
                      </Typography>
                    </Stack>
                  }
                  secondary={
                    <Typography component="span" variant="body2" color="text.secondary">
                      {pago.proveedor} · {pago.referencia}
                    </Typography>
                  }
                />
              </ListItem>
            ))}
          </List>
        )}
      </Box>
    </SectionCard>
  )
}
