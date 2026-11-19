import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Skeleton from '@mui/material/Skeleton'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline'
import InboxOutlinedIcon from '@mui/icons-material/InboxOutlined'
import { api } from '../api/client'
import type { MedioMagnetico } from '../api/client'
import EstadoChip from '../components/ui/EstadoChip'
import EmptyState from '../components/ui/EmptyState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import { srOnly } from '../components/ui/srOnly'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

export default function DashboardPage() {
  const navigate = useNavigate()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [medios, setMedios] = useState<MedioMagnetico[]>([])
  const [cargando, setCargando] = useState(true)

  useEffect(() => {
    if (empresaId == null) {
      setMedios([])
      setCargando(false)
      return
    }
    setCargando(true)
    api
      .get<MedioMagnetico[]>(`/mediomagnetico/empresa/${empresaId}`)
      .then((res) => setMedios(res.data))
      .catch(() => setMedios([]))
      .finally(() => setCargando(false))
  }, [empresaId])

  const sinErrores = medios.filter((m) => m.estado === 'VALIDADO' || m.estado === 'XML_GENERADO').length
  const conErrores = medios.filter((m) => m.estado === 'VALIDADO_CON_ERRORES').length
  const valorMetrica = (valor: number) =>
    cargando ? <Skeleton variant="text" width={64} height={44} /> : valor

  return (
    <Box component="section" sx={{ display: 'grid', gap: { xs: 3, md: 4 } }}>
      <PageHeader
        overline="Información exógena"
        title="Panel de reportes"
        subtitle="Resumen de los medios magnéticos cargados para la empresa activa."
        actions={
          <Button variant="contained" onClick={() => navigate('/carga')}>
            Cargar datos
          </Button>
        }
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Reportes del año gravable"
            value={valorMetrica(medios.length)}
            tone="primary"
            icon={<DescriptionOutlinedIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Sin errores de validación"
            value={valorMetrica(sinErrores)}
            tone="success"
            icon={<CheckCircleOutlineIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Con errores por corregir"
            value={valorMetrica(conErrores)}
            tone="warning"
            icon={<ErrorOutlineIcon />}
          />
        </Grid>
      </Grid>

      {!cargandoEmpresa && empresaId == null && (
        <Alert severity="warning">
          No tiene una empresa asignada. Contacte al administrador para vincular su cuenta a una empresa.
        </Alert>
      )}

      <SectionCard title="Medios magnéticos" subtitle="Seleccione un reporte para validarlo o ver su detalle.">
        {cargando && (
          <Box role="status" aria-live="polite" aria-busy="true">
            <Box sx={srOnly}>Cargando medios magnéticos…</Box>
            <Stack spacing={1.5} aria-hidden="true">
              {[0, 1, 2].map((fila) => (
                <Box
                  key={fila}
                  sx={{
                    display: 'flex',
                    flexWrap: 'wrap',
                    gap: 2,
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    p: 2,
                    borderRadius: 2,
                    border: '1px solid',
                    borderColor: (t) => t.palette.divider,
                  }}
                >
                  <Box sx={{ display: 'grid', gap: 0.5, flex: 1, minWidth: 180 }}>
                    <Skeleton variant="text" width="45%" />
                    <Skeleton variant="text" width="65%" />
                  </Box>
                  <Skeleton variant="rounded" width={92} height={34} />
                </Box>
              ))}
            </Stack>
          </Box>
        )}

        {!cargando && medios.length === 0 && (
          <EmptyState
            icon={<InboxOutlinedIcon />}
            title="Sin reportes cargados"
            description="Aún no hay medios magnéticos para esta empresa. Cargue un archivo Excel para comenzar."
            action={
              <Button variant="contained" onClick={() => navigate('/carga')}>
                Cargar datos
              </Button>
            }
          />
        )}

        {!cargando && medios.length > 0 && (
          <Stack spacing={1.5}>
            {medios.map((m) => (
              <Box
                key={m.id}
                sx={{
                  display: 'flex',
                  flexWrap: 'wrap',
                  gap: 2,
                  alignItems: 'center',
                  justifyContent: 'space-between',
                  p: 2,
                  borderRadius: 2,
                  border: '1px solid',
                  borderColor: (t) => t.palette.divider,
                  transition: 'border-color 180ms ease, background-color 180ms ease',
                  '&:hover': {
                    borderColor: (t) => t.palette.primary.main,
                    bgcolor: (t) => t.palette.action.hover,
                  },
                }}
              >
                <Box sx={{ minWidth: 0 }}>
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
        )}
      </SectionCard>
    </Box>
  )
}
