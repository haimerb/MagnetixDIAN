import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import type { ReactNode } from 'react'
import EmptyState from '../../components/ui/EmptyState'
import SectionCard from '../../components/ui/SectionCard'

type CatalogosTabProps = {
  titulo: string
  descripcion: string
  icono: ReactNode
  mensaje: string
}

export default function CatalogosTab({ titulo, descripcion, icono, mensaje }: CatalogosTabProps) {
  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Alert severity="info">
        Esta sección muestra el estado del catálogo. El backend aún no expone un listado consultable;
        no se muestran datos simulados.
      </Alert>
      <SectionCard title={titulo} subtitle={descripcion}>
        <EmptyState icon={icono} title="Catálogo sin consulta pública" description={mensaje} />
      </SectionCard>
    </Box>
  )
}
