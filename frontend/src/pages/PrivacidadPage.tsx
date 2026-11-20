import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import Link from '@mui/material/Link'
import { Link as RouterLink } from 'react-router-dom'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'

const SECCIONES = [
  {
    titulo: '1. Responsable del tratamiento',
    texto:
      'MagnetixDIAN actúa como responsable de los datos de cuenta y como encargado de los datos de terceros que el usuario carga para el cumplimiento de sus obligaciones tributarias.',
  },
  {
    titulo: '2. Datos que procesamos',
    texto:
      'Procesamos datos de registro (nombre, correo, usuario y empresa), datos de facturación y elementos de las operaciones informadas a la DIAN, además de registros de auditoría y de acceso.',
  },
  {
    titulo: '3. Finalidad',
    texto:
      'Los datos se usan para proveer el servicio, validar y generar archivos, emitir facturas, gestionar suscripciones, enviar notificaciones operativas y cumplir obligaciones legales.',
  },
  {
    titulo: '4. Destinatarios',
    texto:
      'Los datos pueden ser alojados en proveedores de infraestructura, base de datos y observabilidad. El usuario es responsable de los datos que reporta a la DIAN y de la veracidad de la información presentada.',
  },
  {
    titulo: '5. Conservación',
    texto:
      'Los datos de cuenta se conservan mientras la relación contractual esté vigente. La información tributaria y de auditoría puede conservarse por los plazos legales aplicables, aun después de cancelar la suscripción.',
  },
  {
    titulo: '6. Derechos ARCO',
    texto:
      'El titular puede solicitar acceso, rectificación, cancelación u oposición de sus datos mediante el canal de soporte indicado en su factura. Atenderemos la solicitud dentro de los plazos establecidos por la ley colombiana.',
  },
  {
    titulo: '7. Seguridad',
    texto:
      'Aplicamos medidas técnicas y organizativas razonables: cifrado en tránsito, contraseñas con hash, control de acceso por empresa, auditoría de eventos y rotación de credenciales de sesión.',
  },
  {
    titulo: '8. Cambios',
    texto:
      'Cualquier modificación a esta política se publicará en esta página con su fecha de actualización. El uso posterior del servicio implica la aceptación de la versión vigente.',
  },
]

export default function PrivacidadPage() {
  return (
    <Box component="main" sx={{ minHeight: '100vh', bgcolor: 'background.default', py: 6 }}>
      <Container maxWidth="md">
        <Paper elevation={0} sx={{ p: { xs: 3, md: 5 }, border: '1px solid', borderColor: 'divider' }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Política de privacidad y tratamiento de datos
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 4 }}>
            Última actualización: noviembre de 2026.
          </Typography>
          {SECCIONES.map((seccion) => (
            <Box key={seccion.titulo} sx={{ mb: 3 }}>
              <Typography variant="h6" gutterBottom>
                {seccion.titulo}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                {seccion.texto}
              </Typography>
            </Box>
          ))}
          <Link component={RouterLink} to="/">
            Volver al inicio
          </Link>
        </Paper>
      </Container>
    </Box>
  )
}
