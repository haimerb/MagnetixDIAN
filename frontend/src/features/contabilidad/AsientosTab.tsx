import { useCallback, useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import {
  conciliarContabilidad,
  listarAsientosContables,
  type AsientoContable,
  type FiltrosConciliacion,
  type ResultadoConciliacion,
} from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import { money } from '../facturacion/calculos'
import ConciliacionTab from './ConciliacionTab'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type AsientosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

const COLUMNAS: ColumnaTabla[] = [
  { id: 'numero', label: 'Asiento' },
  { id: 'fecha', label: 'Fecha' },
  { id: 'tipoEvento', label: 'Evento' },
  { id: 'cuenta', label: 'Cuenta' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
]

export default function AsientosTab({ empresaId, cargandoEmpresa }: AsientosTabProps) {
  const [asientos, setAsientos] = useState<AsientoContable[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [conciliacion, setConciliacion] = useState<ResultadoConciliacion | null>(null)
  const [conciliando, setConciliando] = useState(false)
  const [errorConciliacion, setErrorConciliacion] = useState<string | null>(null)

  const cargarAsientos = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    listarAsientosContables(empresaId)
      .then((datos) => setAsientos(datos))
      .catch(() => setError('No se pudieron cargar los asientos contables.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargarAsientos()
  }, [cargarAsientos])

  const filas = useMemo<FilaTabla[]>(
    () =>
      asientos.flatMap((asiento) =>
        asiento.lineas.length > 0
          ? asiento.lineas.map((linea, indice) => ({
              id: `${asiento.id}-${linea.id}`,
              celdas: {
                numero: indice === 0 ? asiento.numero : '',
                fecha: asiento.fecha,
                tipoEvento: asiento.tipoEvento,
                cuenta: linea.codigoCuenta,
                descripcion: linea.descripcion ?? linea.nombreCuenta,
                debito: money(linea.debito),
                credito: money(linea.credito),
              },
            }))
          : [
              {
                id: String(asiento.id),
                celdas: {
                  numero: asiento.numero,
                  fecha: asiento.fecha,
                  tipoEvento: asiento.tipoEvento,
                  cuenta: '—',
                  descripcion: asiento.descripcion,
                  debito: money(asiento.totalDebito),
                  credito: money(asiento.totalCredito),
                },
              },
            ],
      ),
    [asientos],
  )

  const totales = useMemo(
    () =>
      asientos.reduce(
        (acumulado, asiento) => ({
          debito: acumulado.debito + (asiento.totalDebito ?? 0),
          credito: acumulado.credito + (asiento.totalCredito ?? 0),
        }),
        { debito: 0, credito: 0 },
      ),
    [asientos],
  )

  const onConciliar = useCallback(
    async (filtros: FiltrosConciliacion) => {
      if (empresaId == null) return
      setConciliando(true)
      setErrorConciliacion(null)
      try {
        setConciliacion(await conciliarContabilidad(empresaId, filtros))
      } catch {
        setErrorConciliacion('No se pudo ejecutar la conciliación contable.')
      } finally {
        setConciliando(false)
      }
    },
    [empresaId],
  )

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <SectionCard
        title="Libro diario"
        subtitle="Asientos generados desde documentos DIAN y operaciones ERP"
        padded={false}
      >
        <TablaDatos
          ariaLabel="Asientos contables"
          columnas={COLUMNAS}
          filas={filas}
          cargando={cargando || cargandoEmpresa}
          minWidth={1040}
          tituloVacio="Sin asientos contables"
          descripcionVacio="Todavía no hay asientos generados para esta empresa."
        />
      </SectionCard>
      <Alert severity="info">
        {asientos.length} asiento(s) · Débitos {money(totales.debito)} · Créditos {money(totales.credito)}
      </Alert>
      <Typography component="h2" variant="h6">
        Conciliación detallada
      </Typography>
      {conciliacion && <Alert severity="success">Conciliación contable completada.</Alert>}
      <ConciliacionTab
        empresaId={empresaId}
        cargandoEmpresa={cargandoEmpresa}
        conciliacion={conciliacion}
        conciliando={conciliando}
        error={errorConciliacion}
        onConciliar={onConciliar}
      />
    </Box>
  )
}
