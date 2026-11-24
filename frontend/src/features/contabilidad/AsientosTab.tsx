import { useEffect, useMemo, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'

type AsientosTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

export default function AsientosTab({ empresaId, cargandoEmpresa }: AsientosTabProps) {
  const [asientos, setAsientos] = useState<Array<{id: number, codigoCuenta: string, nombreCuenta: string, descripcion: string, totalDebito: number, totalCredito: number}>>([])

  useEffect(() => {
    // Cargar asientos del backend
  }, [empresaId])

  const totales = useMemo(
    () => ({
      debito: asientos.reduce((suma, asiento) => suma + (asiento.totalDebito ?? 0), 0),
      credito: asientos.reduce((suma, asiento) => suma + (asiento.totalCredito ?? 0), 0),
    }),
    [asientos]
  )

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Alert severity="info">
        Gestión de asientos contables
      </Alert>
      <p>Total de asientos: {asientos.length}</p>
      <p>Débitos totales: ${totales.debito.toFixed(2)}</p>
      <p>Créditos totales: ${totales.credito.toFixed(2)}</p>
    </Box>
  )
}
