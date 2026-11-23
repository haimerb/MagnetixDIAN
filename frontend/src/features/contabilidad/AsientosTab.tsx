import { useCallback, useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Grid from '@mui/material/Grid2'
import AccountBalanceWalletOutlinedIcon from '@mui/icons-material/AccountBalanceWalletOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import { listarAsientosContables } from '../../api/client'
import type { AsientoContable } from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'
import StatCard from '../../components/ui/StatCard'
import { money } from '../facturacion/calculos'
import TablaDatos, { type ColumnaTabla, type FilaTabla } from './TablaDatos'

type AsientosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

const COLUMNAS_ASIENTOS: ColumnaTabla[] = [
  { id: 'numero', label: 'Número' },
  { id: 'fecha', label: 'Fecha' },
  { id: 'tipoEvento', label: 'Evento' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
]

const COLUMNAS_LINEAS: ColumnaTabla[] = [
  { id: 'codigoCuenta', label: 'Cuenta' },
  { id: 'nombreCuenta', label: 'Nombre' },
  { id: 'descripcion', label: 'Descripción' },
  { id: 'debito', label: 'Débito', align: 'right' },
  { id: 'credito', label: 'Crédito', align: 'right' },
]

export default function AsientosTab({ empresaId, cargandoEmpresa }: AsientosTabProps) {
  const [asientos, setAsientos] = useState<AsientoContable[]>([])
  const [seleccionado, setSeleccionado] = useState<number | null>(null)
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const cargar = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    listarAsientosContables(empresaId)
      .then((datos) => {
        setAsientos(datos)
        setSeleccionado((actual) => actual ?? datos[0]?.id ?? null)
      })
      .catch(() => setError('No se pudieron cargar los asientos contables.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargar()
  }, [cargar])

  const detalle = useMemo(
    () => asientos.find((asiento) => asiento.id === seleccionado) ?? null,
    [asientos, seleccionado],
  )

  const totales = useMemo(
    () => ({
      debito: asientos.reduce((suma, asiento) => suma + (asiento.totalDebito ?? 0), 0),
      credito: asientos.reduce((suma, asiento) => suma + (asiento.totalCredito ?? 0), 0),
    }),
    [asientos],
  )

  const filasAsientos: FilaTabla[] = asientos.map((asiento) => ({
    id: asiento.id,
    celdas: {
      numero: asiento.numero,
      fecha: asiento.fecha,
      tipoEvento: asiento.tipoEvento,
      descripcion: asiento.descripcion,
      debito: money(asiento.totalDebito),
      credito: money(asiento.totalCredito),
    },
  }))

  const filasLineas: FilaTabla[] = (detalle?.lineas ?? []).map((linea) => ({
    id: linea.id,
    celdas: {
      codigoCuenta: linea.codigoCuenta,
      nombreCuenta: linea.nombreCuenta,
      descripcion: linea.descripcion ?? '—',
      debito: money(linea.debito),
      credito: money(linea.credito),
    },
  }))

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Asientos" value={asientos.length} icon={<ReceiptLongOutlinedIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Total débito"
            value={money(totales.debito)}
            tone="info"
            icon={<AccountBalanceWalletOutlinedIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Total crédito"
            value={money(totales.credito)}
            tone="success"
            icon={<FactCheckOutlinedIcon />}
          />
        </Grid>
      </Grid>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar la contabilidad.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Asientos" subtitle="Seleccione un asiento para ver sus líneas" padded={false}>
        <TablaDatos
          ariaLabel="Asientos contables"
          columnas={COLUMNAS_ASIENTOS}
          filas={filasAsientos}
          cargando={cargando || cargandoEmpresa}
          iconoVacio={<ReceiptLongOutlinedIcon />}
          tituloVacio="Sin asientos contables"
          descripcionVacio="Traiga documentos de la DIAN, cáuselos y asíntelos desde Asentar facturación."
          onSeleccionar={(id) => setSeleccionado(Number(id))}
          seleccionado={seleccionado}
        />
      </SectionCard>

      <SectionCard
        title="Líneas del asiento"
        subtitle={detalle ? `${detalle.numero} · ${detalle.descripcion}` : undefined}
        padded={false}
      >
        <TablaDatos
          ariaLabel="Líneas del asiento"
          columnas={COLUMNAS_LINEAS}
          filas={filasLineas}
          cargando={false}
          iconoVacio={<FactCheckOutlinedIcon />}
          tituloVacio="Sin asiento seleccionado"
        />
      </SectionCard>
    </Box>
  )
}
