import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import TextField from '@mui/material/TextField'
import { useEffect, useState } from 'react'
import { listarCuentasPuc } from '../../api/client'
import type { CuentaPuc } from '../../api/client'
import SectionCard from '../../components/ui/SectionCard'

type CatalogosTabProps = {
  titulo: string
  descripcion: string
}

export default function CatalogosTab({ titulo, descripcion }: CatalogosTabProps) {
  const [cuentas, setCuentas] = useState<CuentaPuc[]>([])
  const [buscando, setBuscando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [filtrando, setFiltrando] = useState('')

  useEffect(() => {
    setBuscando(true)
    setError(null)
    listarCuentasPuc(1)
      .then((datos) => {
        setCuentas(datos)
        setBuscando(false)
      })
      .catch((e) => {
        setError(e instanceof Error ? e.message : 'Error desconocido')
        setBuscando(false)
      })
  }, [])

  const cuentasFiltradas = cuentas.filter((c) =>
    c.codigo.toLowerCase().includes(filtrando.toLowerCase()) ||
    c.nombre.toLowerCase().includes(filtrando.toLowerCase())
  )

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <Alert severity="info">
        Catálogo de cuentas contables oficial NIIF 2025. Utiliza el buscador para encontrar cuentas.
      </Alert>
      <SectionCard title={titulo} subtitle={descripcion}>
        <TextField
          label="Buscar cuenta"
          variant="outlined"
          value={filtrando}
          onChange={(e) => setFiltrando(e.target.value)}
          sx={{ mb: 2 }}
        />
        {buscando && <p>Cargando cuentas...</p>}

        <Box sx={{ overflowX: 'auto' }}>
          <p>Catálogo PUC - {cuentas.length} cuentas cargadas</p>
        </Box>
      </SectionCard>
      {error && <Alert severity="error">{error}</Alert>}
    </Box>
  )
}
