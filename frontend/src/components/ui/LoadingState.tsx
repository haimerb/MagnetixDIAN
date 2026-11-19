import Box from '@mui/material/Box'
import CircularProgress from '@mui/material/CircularProgress'
import Typography from '@mui/material/Typography'

type LoadingStateProps = {
  label?: string
  minHeight?: number | string
  compacto?: boolean
}

export default function LoadingState({
  label = 'Cargando…',
  minHeight = '40vh',
  compacto = false,
}: LoadingStateProps) {
  return (
    <Box
      role="status"
      aria-live="polite"
      aria-busy="true"
      sx={{
        display: 'grid',
        placeItems: 'center',
        gap: 1.5,
        minHeight,
        py: compacto ? 2 : 4,
      }}
    >
      <CircularProgress size={compacto ? 20 : 32} />
      <Typography variant="body2" color="text.secondary">
        {label}
      </Typography>
    </Box>
  )
}
