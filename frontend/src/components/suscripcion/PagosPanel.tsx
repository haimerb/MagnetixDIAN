import { useCallback, useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import SectionCard from '../ui/SectionCard'
import EmptyState from '../ui/EmptyState'
import EstadoChip from '../ui/EstadoChip'
import { listarPagos, type Pago } from '../../api/client'

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
                secondaryAction={<EstadoChip estado={pago.estado} />}
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
