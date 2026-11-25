import { useEffect, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Chip from '@mui/material/Chip'
import Container from '@mui/material/Container'
import Grid from '@mui/material/Grid2'
import Link from '@mui/material/Link'
import Typography from '@mui/material/Typography'
import { alpha } from '@mui/material/styles'
import Logo from '../components/ui/Logo'
import { listarPlanes, type Plan } from '../api/client'

const BENEFICIOS = [
  {
    titulo: 'Facturación electrónica',
    texto: 'Emita, envíe y haga seguimiento de facturas y notas DIAN.',
  },
  {
    titulo: 'Contabilidad integrada',
    texto: 'Cause documentos y genere asientos automáticos desde cada factura.',
  },
  {
    titulo: 'Exógena DIAN',
    texto: 'Valide el formato 1001 y genere el XML listo para MUISCA.',
  },
  {
    titulo: 'Cobro y activación',
    texto: 'Checkout, firma de webhook verificada y tenant listo para facturar.',
  },
  {
    titulo: 'Multiempresa',
    texto: 'Administre varias compañías con acceso por empresa y roles.',
  },
  {
    titulo: 'Auditoría y seguridad',
    texto: 'Registro de eventos, tokens con rotación y credenciales cifradas.',
  },
]

const PASOS = [
  'Cree su cuenta y registre su empresa.',
  'Pague y active su tenant automáticamente.',
  'Habilite su PAC o configure el modo cliente DIAN.',
  'Facture, contabilice y genere su exógena.',
]

const PLANES_FALLBACK: Plan[] = [
  {
    id: 1,
    codigo: 'FREE',
    nombre: 'Gratis',
    descripcion: 'Hasta 50 documentos al mes y 1 usuario.',
    precioMensual: 0,
    moneda: 'COP',
    limiteFacturasMes: 50,
    limiteUsuarios: 1,
    orden: 1,
  },
]

function precio(plan: Plan) {
  if (plan.precioMensual === 0) return 'Gratis'
  return new Intl.NumberFormat('es-CO', {
    style: 'currency',
    currency: plan.moneda || 'COP',
    maximumFractionDigits: 0,
  }).format(plan.precioMensual)
}

export default function LandingPage() {
  const [planes, setPlanes] = useState<Plan[]>(PLANES_FALLBACK)

  useEffect(() => {
    let activo = true
    listarPlanes()
      .then((respuesta) => {
        if (activo && Array.isArray(respuesta) && respuesta.length > 0) setPlanes(respuesta)
      })
      .catch(() => undefined)
    return () => {
      activo = false
    }
  }, [])

  return (
    <Box component="main" sx={{ bgcolor: 'background.default' }}>
      <Box
        component="header"
        sx={{
          background: (t) =>
            `linear-gradient(150deg, ${t.palette.primary.dark}, ${t.palette.primary.main} 58%, ${t.palette.primary.light})`,
          color: 'common.white',
        }}
      >
        <Container maxWidth="lg">
          <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', py: 2.5 }}>
            <Logo size={36} variant="onDark" />
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Button component={RouterLink} to="/login" color="inherit">
                Ingresar
              </Button>
              <Button component={RouterLink} to="/registro" variant="contained" color="secondary">
                Crear cuenta gratis
              </Button>
            </Box>
          </Box>

          <Grid container spacing={4} sx={{ py: { xs: 6, md: 10 } }} alignItems="center">
            <Grid size={{ xs: 12, md: 7 }}>
              <Chip
                label="ERP contable + facturación electrónica + exógena DIAN"
                sx={{ mb: 3, bgcolor: (t) => alpha(t.palette.common.white, 0.16), color: 'common.white' }}
              />
              <Typography variant="h2" component="h1" sx={{ color: 'common.white', mb: 2 }}>
                De la venta a la factura, sin fricción
              </Typography>
              <Typography variant="h6" sx={{ color: 'common.white', opacity: 0.86, mb: 4, maxWidth: 620 }}>
                Facture, cause y contabilice en un solo lugar. Cumpla con la DIAN y cierre su mes sin
                hojas de cálculo ni reprocesos.
              </Typography>
              <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap' }}>
                <Button component={RouterLink} to="/registro" variant="contained" color="secondary" size="large">
                  Empezar gratis
                </Button>
                <Button component={RouterLink} to="/login" variant="outlined" color="inherit" size="large">
                  Ya tengo cuenta
                </Button>
              </Box>
              <Typography variant="body2" sx={{ color: (t) => alpha(t.palette.common.white, 0.72), mt: 3 }}>
                Sin tarjeta de crédito · Plan gratuito incluido · Configure su PAC cuando lo necesite
              </Typography>
            </Grid>
            <Grid size={{ xs: 12, md: 5 }}>
              <Card elevation={0} sx={{ p: 2, bgcolor: 'common.white' }}>
                <CardContent>
                  <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                    Así funciona
                  </Typography>
                  {PASOS.map((paso, indice) => (
                    <Box key={paso} sx={{ display: 'flex', gap: 2, alignItems: 'flex-start', mt: 2 }}>
                      <Chip label={indice + 1} color="primary" size="small" />
                      <Typography variant="body2">{paso}</Typography>
                    </Box>
                  ))}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Container>
      </Box>

      <Container maxWidth="lg" sx={{ py: { xs: 6, md: 9 } }}>
        <Typography variant="h4" component="h2" align="center" gutterBottom>
          Todo lo que su operación necesita para facturar y cerrar
        </Typography>
        <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 6 }}>
          Diseñado para empresas colombianas que venden, facturan y deben cumplir con la DIAN.
        </Typography>
        <Grid container spacing={3}>
          {BENEFICIOS.map((beneficio) => (
            <Grid key={beneficio.titulo} size={{ xs: 12, sm: 6, md: 4 }}>
              <Card elevation={0} sx={{ height: '100%', border: '1px solid', borderColor: 'divider' }}>
                <CardContent>
                  <Typography variant="h6" gutterBottom>
                    {beneficio.titulo}
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    {beneficio.texto}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      </Container>

      <Box sx={{ bgcolor: 'action.hover', py: { xs: 6, md: 9 } }} component="section" id="planes">
        <Container maxWidth="lg">
          <Typography variant="h4" component="h2" align="center" gutterBottom>
            Planes simples
          </Typography>
          <Typography variant="body1" color="text.secondary" align="center" sx={{ mb: 6 }}>
            Empiece gratis y crezca cuando su operación lo requiera.
          </Typography>
          <Grid container spacing={3} justifyContent="center">
            {planes.map((plan) => (
              <Grid key={plan.id} size={{ xs: 12, sm: 6, md: 4 }}>
                <Card
                  elevation={0}
                  sx={{
                    height: '100%',
                    p: 2,
                    border: '2px solid',
                    borderColor: plan.codigo === 'FREE' ? 'primary.main' : 'divider',
                  }}
                >
                  <CardContent>
                    <Typography variant="h6" gutterBottom>
                      {plan.nombre}
                    </Typography>
                    <Typography variant="h4" gutterBottom>
                      {precio(plan)}
                    </Typography>
                    <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
                      {plan.descripcion}
                    </Typography>
                    <Button
                      component={RouterLink}
                      to={`/registro?plan=${encodeURIComponent(plan.codigo)}`}
                      fullWidth
                      variant="contained"
                    >
                      {plan.codigo === 'FREE' ? 'Empezar gratis' : 'Crear cuenta'}
                    </Button>
                  </CardContent>
                </Card>
              </Grid>
            ))}
          </Grid>
        </Container>
      </Box>

      <Box component="footer" sx={{ borderTop: '1px solid', borderColor: 'divider', py: 4 }}>
        <Container maxWidth="lg">
          <Box sx={{ display: 'flex', justifyContent: 'space-between', flexWrap: 'wrap', gap: 2 }}>
            <Typography variant="caption" color="text.secondary">
              MagnetixDIAN · ERP contable y facturación electrónica · Exógena DIAN
            </Typography>
            <Box sx={{ display: 'flex', gap: 2 }}>
              <Link component={RouterLink} to="/terminos" variant="caption">
                Términos
              </Link>
              <Link component={RouterLink} to="/privacidad" variant="caption">
                Privacidad
              </Link>
              <Link component={RouterLink} to="/login" variant="caption">
                Ingresar
              </Link>
            </Box>
          </Box>
        </Container>
      </Box>
    </Box>
  )
}
