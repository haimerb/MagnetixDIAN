import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import TextField from '@mui/material/TextField'
import CalculateOutlinedIcon from '@mui/icons-material/CalculateOutlined'
import {
  calcularImpuesto,
  listarCalendarioTributario,
  listarCausacionesImpuesto,
  listarDeclaracionesImpuesto,
  listarResponsabilidadesFiscales,
  listarTarifasImpuesto,
  type CalendarioTributario,
  type CausacionImpuesto,
  type DeclaracionImpuesto,
  type ResponsabilidadFiscal,
  type ResultadoCalculoImpuesto,
  type TarifaImpuesto,
} from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import EstadoChip from '../../components/ui/EstadoChip'
import { money } from '../facturacion/calculos'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type ImpuestosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

const COLUMNAS_TARIFAS: ColumnaTabla[] = [
  { id: 'impuesto', label: 'Impuesto' },
  { id: 'concepto', label: 'Concepto' },
  { id: 'anio', label: 'Año' },
  { id: 'porcentaje', label: 'Tarifa', align: 'right' },
  { id: 'activo', label: 'Estado' },
]

const COLUMNAS_CAUSACIONES: ColumnaTabla[] = [
  { id: 'periodo', label: 'Periodo' },
  { id: 'impuesto', label: 'Impuesto' },
  { id: 'concepto', label: 'Concepto' },
  { id: 'base', label: 'Base', align: 'right' },
  { id: 'valor', label: 'Valor', align: 'right' },
  { id: 'estado', label: 'Estado' },
]

const COLUMNAS_DECLARACIONES: ColumnaTabla[] = [
  { id: 'periodo', label: 'Periodo' },
  { id: 'impuesto', label: 'Impuesto' },
  { id: 'limite', label: 'Fecha límite' },
  { id: 'causado', label: 'Causado', align: 'right' },
  { id: 'pagado', label: 'Pagado', align: 'right' },
  { id: 'saldo', label: 'Saldo', align: 'right' },
  { id: 'estado', label: 'Estado' },
]

const COLUMNAS_CALENDARIO: ColumnaTabla[] = [
  { id: 'periodo', label: 'Periodo' },
  { id: 'impuesto', label: 'Impuesto' },
  { id: 'periodicidad', label: 'Periodicidad' },
  { id: 'limite', label: 'Fecha límite' },
]

export default function ImpuestosTab({ empresaId, cargandoEmpresa }: ImpuestosTabProps) {
  const anio = new Date().getFullYear()
  const [tarifas, setTarifas] = useState<TarifaImpuesto[]>([])
  const [causaciones, setCausaciones] = useState<CausacionImpuesto[]>([])
  const [declaraciones, setDeclaraciones] = useState<DeclaracionImpuesto[]>([])
  const [calendario, setCalendario] = useState<CalendarioTributario[]>([])
  const [responsabilidades, setResponsabilidades] = useState<ResponsabilidadFiscal[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [impuestoCalculo, setImpuestoCalculo] = useState('IVA')
  const [conceptoCalculo, setConceptoCalculo] = useState('TARIFA_GENERAL')
  const [baseCalculo, setBaseCalculo] = useState('1000000')
  const [resultadoCalculo, setResultadoCalculo] = useState<ResultadoCalculoImpuesto | null>(null)
  const [calculando, setCalculando] = useState(false)

  useEffect(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    Promise.all([
      listarTarifasImpuesto(empresaId, anio),
      listarCausacionesImpuesto(empresaId),
      listarDeclaracionesImpuesto(empresaId, anio),
      listarCalendarioTributario(empresaId, anio),
      listarResponsabilidadesFiscales(empresaId),
    ])
      .then(([tarifasData, causacionesData, declaracionesData, calendarioData, responsabilidadesData]) => {
        setTarifas(tarifasData)
        setCausaciones(causacionesData)
        setDeclaraciones(declaracionesData)
        setCalendario(calendarioData)
        setResponsabilidades(responsabilidadesData)
      })
      .catch(() => setError('No se pudo cargar la información tributaria.'))
      .finally(() => setCargando(false))
  }, [anio, empresaId])

  const calcular = async () => {
    if (empresaId == null) return
    setCalculando(true)
    try {
      setResultadoCalculo(
        await calcularImpuesto(empresaId, {
          impuestoCodigo: impuestoCalculo,
          anio,
          conceptoCodigo: conceptoCalculo,
          base: Number(baseCalculo) || 0,
        }),
      )
    } catch {
      setError('No se pudo calcular el impuesto.')
    } finally {
      setCalculando(false)
    }
  }

  const filasTarifas: FilaTabla[] = tarifas.map((tarifa) => ({
    id: tarifa.id,
    celdas: {
      impuesto: tarifa.impuestoCodigo,
      concepto: tarifa.conceptoCodigo,
      anio: tarifa.anio,
      porcentaje: `${tarifa.porcentaje}%`,
      activo: <EstadoChip estado={tarifa.activo ? 'ACTIVO' : 'PENDIENTE'} />,
    },
  }))

  const filasCausaciones: FilaTabla[] = causaciones.map((causacion) => ({
    id: causacion.id,
    celdas: {
      periodo: causacion.periodo,
      impuesto: causacion.impuestoCodigo,
      concepto: causacion.conceptoCodigo,
      base: money(causacion.baseImponible),
      valor: money(causacion.valor),
      estado: <EstadoChip estado={causacion.estado} />,
    },
  }))

  const filasDeclaraciones: FilaTabla[] = declaraciones.map((declaracion) => ({
    id: declaracion.id,
    celdas: {
      periodo: declaracion.periodo,
      impuesto: declaracion.impuestoCodigo,
      limite: declaracion.fechaLimite,
      causado: money(declaracion.totalCausado),
      pagado: money(declaracion.totalPagado),
      saldo: money(declaracion.saldo),
      estado: <EstadoChip estado={declaracion.estado} />,
    },
  }))

  const filasCalendario: FilaTabla[] = calendario.map((item) => ({
    id: item.id,
    celdas: {
      periodo: item.periodo,
      impuesto: item.impuestoCodigo,
      periodicidad: item.periodicidad,
      limite: item.fechaLimite,
    },
  }))

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard label="Tarifas activas" value={tarifas.filter((tarifa) => tarifa.activo).length} icon={<CalculateOutlinedIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard label="Causaciones" value={causaciones.length} tone="info" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard label="Declaraciones" value={declaraciones.length} tone="success" />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <StatCard label="Responsabilidades" value={responsabilidades.length} tone="warning" />
        </Grid>
      </Grid>

      <SectionCard title="Calculadora de impuestos" subtitle="Tarifa vigente según año, impuesto y concepto">
        <Grid container spacing={2} alignItems="center">
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Impuesto"
              value={impuestoCalculo}
              onChange={(event) => setImpuestoCalculo(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Concepto"
              value={conceptoCalculo}
              onChange={(event) => setConceptoCalculo(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <TextField
              fullWidth
              size="small"
              label="Base gravable"
              type="number"
              value={baseCalculo}
              onChange={(event) => setBaseCalculo(event.target.value)}
            />
          </Grid>
          <Grid size={{ xs: 12, md: 3 }}>
            <Button fullWidth variant="contained" onClick={calcular} loading={calculando}>
              Calcular
            </Button>
          </Grid>
          {resultadoCalculo && (
            <Grid size={{ xs: 12 }}>
              <Alert severity={resultadoCalculo.aplica ? 'success' : 'info'}>
                {resultadoCalculo.nombreImpuesto} · Base {money(resultadoCalculo.baseGravable)} · Tarifa{' '}
                {resultadoCalculo.tarifa}% · Valor {money(resultadoCalculo.valor)}
                {!resultadoCalculo.aplica && ' · La base no supera el mínimo gravable.'}
              </Alert>
            </Grid>
          )}
        </Grid>
      </SectionCard>

      <SectionCard title="Tarifas" subtitle={`Vigentes para ${anio}`} padded={false}>
        <TablaDatos
          ariaLabel="Tarifas de impuestos"
          columnas={COLUMNAS_TARIFAS}
          filas={filasTarifas}
          cargando={cargando || cargandoEmpresa}
          iconoVacio={<CalculateOutlinedIcon />}
          tituloVacio="Sin tarifas configuradas"
        />
      </SectionCard>

      <SectionCard title="Causaciones" subtitle="Impuesto causado a partir de documentos y pagos" padded={false}>
        <TablaDatos
          ariaLabel="Causaciones de impuestos"
          columnas={COLUMNAS_CAUSACIONES}
          filas={filasCausaciones}
          cargando={cargando}
          tituloVacio="Sin causaciones"
        />
      </SectionCard>

      <SectionCard title="Declaraciones" subtitle="Borradores y declaraciones presentadas" padded={false}>
        <TablaDatos
          ariaLabel="Declaraciones de impuestos"
          columnas={COLUMNAS_DECLARACIONES}
          filas={filasDeclaraciones}
          cargando={cargando}
          tituloVacio="Sin declaraciones"
        />
      </SectionCard>

      <SectionCard title="Calendario tributario" subtitle="Fechas límite del año" padded={false}>
        <TablaDatos
          ariaLabel="Calendario tributario"
          columnas={COLUMNAS_CALENDARIO}
          filas={filasCalendario}
          cargando={cargando}
          tituloVacio="Sin fechas programadas"
        />
      </SectionCard>
    </Box>
  )
}
