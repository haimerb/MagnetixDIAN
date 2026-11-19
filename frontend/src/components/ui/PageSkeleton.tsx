import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Skeleton from '@mui/material/Skeleton'
import { srOnly } from './srOnly'

type PageSkeletonProps = {
  secciones?: number
  label?: string
}

export default function PageSkeleton({ secciones = 2, label = 'Cargando página…' }: PageSkeletonProps) {
  return (
    <Box role="status" aria-live="polite" aria-busy="true" sx={{ display: 'grid', gap: 3 }}>
      <Box sx={srOnly}>{label}</Box>
      <Box aria-hidden="true" sx={{ display: 'grid', gap: 1 }}>
        <Skeleton variant="text" width={110} height={18} />
        <Skeleton variant="text" width="42%" height={44} />
        <Skeleton variant="text" width="65%" />
      </Box>
      {Array.from({ length: secciones }).map((_, seccion) => (
        <Card key={seccion} aria-hidden="true">
          <Box sx={{ p: { xs: 2, md: 3 }, display: 'grid', gap: 2 }}>
            <Skeleton variant="text" width="30%" height={24} />
            <Skeleton variant="rectangular" height={120} />
          </Box>
        </Card>
      ))}
    </Box>
  )
}
