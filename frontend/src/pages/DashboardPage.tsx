import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Grid from '@mui/material/Grid2'
import Paper from '@mui/material/Paper'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import { api } from '../api/client'
import type { MedioMagnetico } from '../api/client'
import EstadoChip from '../components/ui/EstadoChip'

export default function DashboardPage() {
  const navigate = useNavigate()
  const [medios, setMedios] = useState<MedioMagnetico[]>([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    api
      .get<MedioMagnetico[]>('/mediomagnetico/empresa/1')
      .then((res) => setMedios(res.data))
      .catch(() => setMedios([]))
      .finally(() => setCargando(false))
  }, [])

  return (
    <Box component="section" sx={{ display: 'grid', gap: { xs: 3, md: 4 } }}>
      <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 2, alignItems: 'center', justifyContent: 'space-between' }}>
        <Typography variant="h4" component="h1">
          Panel de reportes
        </Typography>
        <Button variant="contained" onClick={() => navigate('/carga')}>
          Cargar datos
        </Button>
      </Box>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                Reportes del año gravable
              </Typography>
              <Typography variant="h3" sx={{ mt: 1 }}>
                {medios.length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                Sin errores de validación
              </Typography>
              <Typography variant="h3" sx={{ mt: 1 }}>
                {medios.filter((m) => m.estado === 'VALIDADO' || m.estado === 'XML_GENERADO').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid size={{ xs: 12, md: 4 }}>
          <Card>
            <CardContent>
              <Typography variant="caption" color="text.secondary">
                Con errores por corregir
              </Typography>
              <Typography variant="h3" sx={{ mt: 1 }}>
                {medios.filter((m) => m.estado === 'VALIDADO_CON_ERRORES').length}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Paper variant="outlined">
        <Stack spacing={1} sx={{ p: { xs: 2, md: 3 } }}>
          <Typography variant="h6" component="h2">
            Medios magnéticos
          </Typography>
          {cargando && <Typography variant="body2" color="text.secondary">Cargando…</Typography>}
          {!cargando && medios.length === 0 && (
            <Typography variant="body2" color="text.secondary">
              No hay reportes cargados. Utilice «Cargar datos» para empezar.
            </Typography>
          )}
          {medios.map((m) => (
            <Box
              key={m.id}
              sx={{
                border: '1px solid divider',
                borderRadius: 2,
                p: 2,
                display: 'flex',
                flexWrap: 'wrap',
                gap: 2,
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <Box>
                <Typography variant="subtitle1">
                  Formato {m.formato} · Año gravable {m.anioGravable}
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  {m.empresaRazonSocial} — NIT {m.empresaNit}
                </Typography>
              </Box>
              <Stack direction="row" spacing={1} alignItems="center">
                <EstadoChip estado={m.estado} />
                <Button size="small" onClick={() => navigate(`/validacion/${m.id}`)}>
                  Validar
                </Button>
                <Button size="small" onClick={() => navigate(`/reporte/${m.id}`)}>
                  Reporte
                </Button>
              </Stack>
            </Box>
          ))}
        </Stack>
      </Paper>
    </Box>
  )
}