import Box from '@mui/material/Box'
import LinearProgress from '@mui/material/LinearProgress'
import { useEffect, useState, useSyncExternalStore } from 'react'
import { peticionesActivas, suscribirProgreso } from '../../api/progreso'

const RETARDO_MS = 150

export default function GlobalProgress() {
  const activas = useSyncExternalStore(suscribirProgreso, peticionesActivas, () => 0)
  const [visible, setVisible] = useState(false)

  useEffect(() => {
    if (activas === 0) {
      setVisible(false)
      return
    }
    const id = window.setTimeout(() => setVisible(true), RETARDO_MS)
    return () => window.clearTimeout(id)
  }, [activas])

  return (
    <Box
      aria-hidden="true"
      sx={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        zIndex: (t) => t.zIndex.tooltip,
        pointerEvents: 'none',
      }}
    >
      <LinearProgress sx={{ height: 3, visibility: visible ? 'visible' : 'hidden' }} />
    </Box>
  )
}
