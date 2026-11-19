import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TablePagination from '@mui/material/TablePagination'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import SyncOutlinedIcon from '@mui/icons-material/SyncOutlined'
import CloudOffOutlinedIcon from '@mui/icons-material/CloudOffOutlined'
import { listarEnvios, reintentarEnvio, type EnvioFactura } from '../../api/client'
import EstadoChip from '../ui/EstadoChip'
import EmptyState from '../ui/EmptyState'
import SectionCard from '../ui/SectionCard'
import TableSkeleton from '../ui/TableSkeleton'

const FILAS = [10, 25, 50]
const ESTADOS = [
  { value: '', label: 'Todos' },
  { value: 'PENDIENTE', label: 'Pendientes' },
  { value: 'AGOTADO', label: 'Agotados' },
  { value: 'EXITOSO', label: 'Enviados' },
]

export default function EnviosPanel() {
  const [envios, setEnvios] = useState<EnvioFactura[]>([])
  const [total, setTotal] = useState(0)
  const [pagina, setPagina] = useState(0)
  const [tamano, setTamano] = useState(FILAS[0])
  const [estado, setEstado] = useState('')
  const [cargando, setCargando] = useState(true)
  const [reintentando, setReintentando] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [aviso, setAviso] = useState<string | null>(null)

  const cargar = () => {
    setCargando(true)
    listarEnvios(estado || undefined, pagina, tamano)
      .then((r) => {
        setEnvios(r.contenido)
        setTotal(r.total)
      })
      .catch(() => setError('No se pudo cargar la cola de envíos.'))
      .finally(() => setCargando(false))
  }

  useEffect(() => {
    let activo = true
    setCargando(true)
    listarEnvios(estado || undefined, pagina, tamano)
      .then((r) => {
        if (!activo) return
        setEnvios(r.contenido)
        setTotal(r.total)
      })
      .catch(() => activo && setError('No se pudo cargar la cola de envíos.'))
      .finally(() => activo && setCargando(false))
    return () => {
      activo = false
    }
  }, [estado, pagina, tamano])

  const reintentar = async (id: number) => {
    setReintentando(id)
    setError(null)
    setAviso(null)
    try {
      const actualizado = await reintentarEnvio(id)
      setAviso(
        actualizado.estado === 'EXITOSO'
          ? `Documento ${actualizado.numeroCompleto} enviado correctamente.`
          : `Reintento ${actualizado.intentos}/${actualizado.maxIntentos}: ${actualizado.ultimoError ?? 'sin respuesta'}`,
      )
      cargar()
    } catch {
      setError('No se pudo reintentar el envío.')
    } finally {
      setReintentando(null)
    }
  }

  return (
    <SectionCard title="Reintentos de transmisión" padded={!cargando && envios.length === 0}>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1} sx={{ p: 2, alignItems: { sm: 'center' } }}>
        <TextField
          select
          label="Estado"
          size="small"
          value={estado}
          onChange={(e) => {
            setEstado(e.target.value)
            setPagina(0)
          }}
          sx={{ minWidth: 180 }}
        >
          {ESTADOS.map((opcion) => (
            <MenuItem key={opcion.value} value={opcion.value}>{opcion.label}</MenuItem>
          ))}
        </TextField>
        <Button variant="outlined" startIcon={<SyncOutlinedIcon />} onClick={cargar}>Actualizar</Button>
      </Stack>
      {error && <Alert severity="error" sx={{ m: 2 }} onClose={() => setError(null)}>{error}</Alert>}
      {aviso && <Alert severity="info" sx={{ m: 2 }} onClose={() => setAviso(null)}>{aviso}</Alert>}
      {cargando ? (
        <TableSkeleton filas={8} columnas={7} label="Cargando envíos…" />
      ) : envios.length === 0 && !error ? (
        <Box sx={{ p: 2 }}>
          <EmptyState
            icon={<CloudOffOutlinedIcon />}
            title="Sin envíos en este estado"
            description="Los documentos rechazados aparecerán aquí para reintentar su transmisión."
          />
        </Box>
      ) : (
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Factura</TableCell>
                <TableCell>Canal</TableCell>
                <TableCell>Estado</TableCell>
                <TableCell align="right">Intentos</TableCell>
                <TableCell>Próximo intento</TableCell>
                <TableCell>Último error</TableCell>
                <TableCell align="right">Acción</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {envios.map((envio) => (
                <TableRow key={envio.id} hover>
                  <TableCell>{envio.numeroCompleto}</TableCell>
                  <TableCell>{envio.canal}</TableCell>
                  <TableCell><EstadoChip estado={envio.estado} /></TableCell>
                  <TableCell align="right">{envio.intentos}/{envio.maxIntentos}</TableCell>
                  <TableCell>{new Date(envio.proximoIntento).toLocaleString('es-CO')}</TableCell>
                  <TableCell sx={{ maxWidth: 260, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {envio.ultimoError ?? '—'}
                  </TableCell>
                  <TableCell align="right">
                    <Button
                      size="small"
                      disabled={envio.estado === 'EXITOSO' || reintentando === envio.id}
                      onClick={() => reintentar(envio.id)}
                    >
                      {reintentando === envio.id ? 'Enviando…' : 'Reintentar'}
                    </Button>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}
      <TablePagination
        component="div"
        count={total}
        page={pagina}
        onPageChange={(_, p) => setPagina(p)}
        rowsPerPage={tamano}
        onRowsPerPageChange={(e) => {
          setTamano(Number(e.target.value))
          setPagina(0)
        }}
        rowsPerPageOptions={FILAS}
        labelRowsPerPage="Filas"
      />
    </SectionCard>
  )
}
