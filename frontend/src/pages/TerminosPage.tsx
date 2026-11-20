import Box from '@mui/material/Box'
import Container from '@mui/material/Container'
import Link from '@mui/material/Link'
import { Link as RouterLink } from 'react-router-dom'
import Paper from '@mui/material/Paper'
import Typography from '@mui/material/Typography'

const SECCIONES = [
  {
    titulo: '1. Objeto',
    texto:
      'MagnetixDIAN permite cargar, validar y generar información exógena para la DIAN, además de emitir facturación electrónica y administrar suscripciones. El uso del servicio implica la aceptación de estos términos.',
  },
  {
    titulo: '2. Registro y cuenta',
    texto:
      'El usuario es responsable de la veracidad de los datos de registro, de conservar sus credenciales y de notificar cualquier uso no autorizado. Cada empresa cuenta con un propietario de cuenta y los permisos que se le asignen.',
  },
  {
    titulo: '3. Planes y pagos',
    texto:
      'El plan gratuito tiene límites de documentos y usuarios. Los planes de pago se activan luego de confirmada la transacción. Las tarifas y condiciones vigentes se muestran en la sección de suscripción antes de confirmar el pago.',
  },
  {
    titulo: '4. Obligaciones del usuario',
    texto:
      'El usuario debe revisar la información antes de presentarla ante la DIAN, conservar los soportes de sus operaciones y cumplir las normas tributarias aplicables. MagnetixDIAN no sustituye la revisión de un contador o revisor fiscal.',
  },
  {
    titulo: '5. Propiedad intelectual',
    texto:
      'El software, los diseños, las reglas de validación y la documentación son propiedad de MagnetixDIAN. Los datos cargados por el usuario permanecen bajo su responsabilidad y titularidad.',
  },
  {
    titulo: '6. Disponibilidad y responsabilidad',
    texto:
      'El servicio se ofrece con esfuerzos razonables de disponibilidad. No se garantiza que el uso de la plataforma evite observaciones de la DIAN derivadas de datos incorrectos, omisiones del usuario o cambios normativos.',
  },
  {
    titulo: '7. Terminación',
    texto:
      'El usuario puede cancelar su suscripción desde la sección correspondiente. La terminación no elimina obligaciones tributarias ni suspendidas por ley, y la retención de datos se rige por la política de privacidad.',
  },
  {
    titulo: '8. Ley aplicable',
    texto:
      'Estos términos se rigen por las leyes de la República de Colombia. Cualquier controversia se resolverá conforme a la jurisdicción competente.',
  },
]

export default function TerminosPage() {
  return (
    <Box component="main" sx={{ minHeight: '100vh', bgcolor: 'background.default', py: 6 }}>
      <Container maxWidth="md">
        <Paper elevation={0} sx={{ p: { xs: 3, md: 5 }, border: '1px solid', borderColor: 'divider' }}>
          <Typography variant="h4" component="h1" gutterBottom>
            Términos y condiciones
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
