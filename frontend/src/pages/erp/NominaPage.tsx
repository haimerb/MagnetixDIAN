import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'

export default function NominaPage() {
  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="ERP"
        title="Nómina"
        subtitle="Preparación de novedades y su posterior contabilización."
      />
      <Alert severity="info">
        El módulo de nómina usa el mismo plan de cuentas y periodos contables que la facturación.
      </Alert>
      <SectionCard title="Novedades de nómina">
        <EmptyState
          title="Sin novedades registradas"
          description="Las novedades de nómina se incorporarán cuando se conecte la fuente de datos de la empresa."
          action={
            <Button component={RouterLink} to="/app/contabilidad?tab=asientos" variant="outlined">
              Ver asientos contables
            </Button>
          }
        />
      </SectionCard>
    </Box>
  )
}
