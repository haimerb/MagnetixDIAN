import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'

export default function DocumentoSoportePage() {
  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Facturación"
        title="Documentos soporte"
        subtitle="Gestión del documento soporte de pago a no obligados a facturar."
      />
      <Alert severity="info">
        El TEST SET DIAN valida facturas, nota débito y nota crédito; el documento soporte se prueba por separado.
      </Alert>
      <SectionCard title="Documentos soporte registrados">
        <EmptyState
          title="Sin documentos soporte"
          description="Los documentos soporte generados desde el ERP aparecerán aquí."
          action={
            <Button component={RouterLink} to="/app/habilitacion-dian" variant="outlined">
              Validar TEST SET DIAN
            </Button>
          }
        />
      </SectionCard>
    </Box>
  )
}
