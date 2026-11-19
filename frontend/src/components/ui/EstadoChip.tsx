import Box from '@mui/material/Box'
import { alpha, useTheme } from '@mui/material/styles'

type Tone = 'neutral' | 'primary' | 'info' | 'success' | 'warning' | 'error'

const ESTADOS: Record<string, { tone: Tone; label: string }> = {
  BORRADOR: { tone: 'neutral', label: 'Borrador' },
  CARGADO: { tone: 'info', label: 'Cargado' },
  VALIDADO: { tone: 'success', label: 'Validado' },
  VALIDADO_CON_ERRORES: { tone: 'warning', label: 'Con errores' },
  XML_GENERADO: { tone: 'primary', label: 'XML generado' },
  PRESENTADO: { tone: 'success', label: 'Presentado' },
  ACEPTADO: { tone: 'success', label: 'Aceptado' },
  RECHAZADO: { tone: 'error', label: 'Rechazado' },
  PENDIENTE: { tone: 'warning', label: 'Pendiente' },
  APROBADO: { tone: 'success', label: 'Aprobado' },
  EXITOSO: { tone: 'success', label: 'Enviado' },
  AGOTADO: { tone: 'error', label: 'Agotado' },
}

export default function EstadoChip({ estado }: { estado: string }) {
  const theme = useTheme()
  const cfg = ESTADOS[estado] ?? { tone: 'neutral' as Tone, label: estado }
  const main =
    cfg.tone === 'neutral' ? theme.palette.text.secondary : theme.palette[cfg.tone].main

  return (
    <Box
      component="span"
      sx={{
        display: 'inline-flex',
        alignItems: 'center',
        gap: 0.75,
        px: 1.25,
        py: 0.25,
        borderRadius: '999px',
        fontSize: '0.75rem',
        fontWeight: 600,
        lineHeight: 1.7,
        whiteSpace: 'nowrap',
        color: main,
        bgcolor: alpha(main, 0.12),
        border: '1px solid',
        borderColor: alpha(main, 0.28),
      }}
    >
      <Box sx={{ width: 7, height: 7, borderRadius: '50%', bgcolor: main }} />
      {cfg.label}
    </Box>
  )
}
