import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import TextField from '@mui/material/TextField'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import type { FiltrosConciliacion, ResultadoConciliacion } from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import EstadoChip from '../../components/ui/EstadoChip'
import EmptyState from '../../components/ui/EmptyState'
import { money } from '../facturacion/calculos'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type ConciliacionTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
  conciliacion: ResultadoConciliacion | null
  conciliando: boolean
  error: string | null
  onConciliar: (filtros: FiltrosConciliacion) => void
}

const COLUMNAS: ColumnaTabla[] = [
  { id: 'consecutivo', label: 'Documento' },
  { id: 'asiento', label: 'Asiento' },
  { id: 'campo', label: 'Campo' },
  { id: 'valorDocumento', label: 'Documento', align: 'right' },
  { id: 'valorAsiento', label: 'Asiento', align: 'right' },
  { id: 'diferencia', label: 'Diferencia', align: 'right' },
  { id: 'resultado', label: 'Resultado' },
  { id: 'mensaje', label: 'Mensaje' },
]

export default function ConciliacionTab({
  empresaId,
  cargandoEmpresa,
  conciliacion,
  conciliando,
  error,
  onConciliar,
}: ConciliacionTabProps) {
  const [fechaDesde, setFechaDesde] = useState('')
  const [fechaHasta, setFechaHasta] = useState('')
  const [direccion, setDireccion] = useState('')
  const [tipoDocumento, setTipoDocumento] = useState('')

  const filas: FilaTabla[] = (conciliacion?.lineas ?? []).map((linea) => ({
    id: `${linea.documentoId}-${linea.asientoId ?? 0}-${linea.campo}-${linea.diferencia}`,
    celdas: {
      consecutivo: linea.consecutivo,
      asiento: linea.asientoId == null ? '—' : `Asiento ${linea.asientoId}`,
      campo: linea.campo,
      valorDocumento: money(linea.valorDocumento),
      valorAsiento: linea.valorAsiento == null ? '—' : money(linea.valorAsiento),
      diferencia: money(linea.diferencia),
      resultado: <EstadoChip estado={linea.resultado} />,
      mensaje: linea.mensaje,
    },
  }))

  const filtros: FiltrosConciliacion = {
    fechaDesde: fechaDesde || undefined,
    fechaHasta: fechaHasta || undefined,
    direccion: direccion || undefined,
    tipoDocumento: tipoDocumento || undefined,
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard title="Filtros de conciliación" subtitle="Compare documentos DIAN con asientos contables">
        <Grid container spacing={2} alignItems="center">
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="Desde"
              slotProps={{ inputLabel: { shrink: true } }}
              value={fechaDesde}
              onChange={(event) => setFechaDesde(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              type="date"
              label="Hasta"
              slotProps={{ inputLabel: { shrink: true } }}
              value={fechaHasta}
              onChange={(event) => setFechaHasta(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Dirección"
              value={direccion}
              onChange={(event) => setDireccion(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <TextField
              fullWidth
              size="small"
              label="Tipo documento"
              value={tipoDocumento}
              onChange={(event) => setTipoDocumento(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 2 }}>
            <Button
              fullWidth
              variant="contained"
              loading={conciliando}
              disabled={empresaId == null}
              onClick={() => onConciliar(filtros)}
            >
              Conciliar
            </Button>
          </Grid>
        </Grid>
      </SectionCard>

      {conciliacion && (
        <Grid container spacing={2}>
          <Grid size={{ xs: 6, md: 2 }}>
            <StatCard label="Coincidencias" value={conciliacion.coincidencias} tone="success" />
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <StatCard label="Diferencias" value={conciliacion.diferencias} tone="error" />
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <StatCard label="Documentos" value={conciliacion.totalDocumentos} tone="info" />
          </Grid>
          <Grid size={{ xs: 6, md: 2 }}>
            <StatCard label="Sin asiento" value={conciliacion.documentosSinAsiento} tone="warning" />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <StatCard label="Valor en diferencias" value={money(conciliacion.valorDiferencia)} />
          </Grid>
        </Grid>
      )}

      <SectionCard title="Resultado de la conciliación" subtitle="Comparación entre documentos y asientos" padded={false}>
        {conciliacion ? (
          <TablaDatos
            ariaLabel="Líneas de conciliación"
            columnas={COLUMNAS}
            filas={filas}
            cargando={conciliando || cargandoEmpresa}
            minWidth={1040}
            iconoVacio={<FactCheckOutlinedIcon />}
            tituloVacio="Sin diferencias"
            descripcionVacio="Todos los documentos concuerdan con sus asientos."
          />
        ) : (
          <EmptyState
            icon={<FactCheckOutlinedIcon />}
            title="Aún no hay conciliación"
            description="Ejecute la conciliación para ver el detalle documento por documento."
            action={
              <Button variant="contained" loading={conciliando} onClick={() => onConciliar(filtros)}>
                Conciliar
              </Button>
            }
          />
        )}
      </SectionCard>
    </Box>
  )
}
