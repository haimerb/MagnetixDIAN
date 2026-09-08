import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Paper from '@mui/material/Paper'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { api } from '../api/client'
import type { CargaResultado } from '../api/client'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

export default function CargaPage() {
  const navigate = useNavigate()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [archivo, setArchivo] = useState<File | null>(null)
  const [anioGravable, setAnioGravable] = useState('2025')
  const [formato, setFormato] = useState('1001')
  const [cargando, setCargando] = useState(false)
  const [resultado, setResultado] = useState<CargaResultado | null>(null)
  const [error, setError] = useState<string | null>(null)

  const onDrop = useCallback((files: File[]) => {
    setArchivo(files[0] ?? null)
    setError(null)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'] },
    maxFiles: 1,
  })

  const onDescargarPlantilla = async () => {
    const token = localStorage.getItem('magnetixdian_token')
    const res = await fetch(`/api/mediomagnetico/plantilla?formato=${formato}`, {
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    })
    if (!res.ok) return
    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    const a = window.document.createElement('a')
    a.href = url
    a.download = `plantilla-${formato}.xlsx`
    a.click()
    URL.revokeObjectURL(url)
  }

  const onCargar = async () => {
    if (!archivo || empresaId == null) return
    setCargando(true)
    setError(null)
    try {
      const form = new FormData()
      form.append('formato', formato)
      form.append('anioGravable', anioGravable)
      form.append('archivo', archivo)
      const { data } = await api.post<CargaResultado>(`/mediomagnetico/${empresaId}/cargar`, form, {
        headers: { 'Content-Type': 'multipart/form-data' },
      })
      setResultado(data)
      setArchivo(null)
    } catch {
      setError('No se pudo cargar el archivo. Verifique que sea un Excel (.xlsx) con el formato esperado.')
    } finally {
      setCargando(false)
    }
  }

  return (
    <Box component="section" sx={{ maxWidth: 720, display: 'grid', gap: 3 }}>
      <Typography variant="h4" component="h1">
        Carga de datos
      </Typography>
      <Typography variant="body2" color="text.secondary">
        Suba el archivo Excel con las operaciones del formato {formato} en la plantilla del prevalidador
        DIAN. Las recargas reemplazan la información existente.{' '}
        <Button size="small" onClick={onDescargarPlantilla} sx={{ textTransform: 'none', p: 0, minWidth: 0, verticalAlign: 'baseline' }}>
          Descargar plantilla (.xlsx)
        </Button>
      </Typography>

      <Paper
        variant="outlined"
        {...getRootProps()}
        sx={{
          p: { xs: 3, md: 5 },
          cursor: 'pointer',
          textAlign: 'center',
          bgcolor: isDragActive ? 'primary.light' : 'background.paper',
          color: isDragActive ? '#fff' : 'text.secondary',
        }}
      >
        <input {...getInputProps()} aria-label="Archivo Excel a cargar" />
        <Typography variant="body1">
          {archivo ? `Archivo: ${archivo.name}` : isDragActive ? 'Suelte el archivo aquí' : 'Arrastre el .xlsx aquí o haga clic'}
        </Typography>
      </Paper>

      <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' } }}>
        <FormControl>
            <InputLabel id="formato-label">Formato</InputLabel>
            <Select labelId="formato-label" label="Formato" value={formato} onChange={(e) => setFormato(e.target.value)}>
              <MenuItem value="1001">1001 — Pagos/abonos y retenciones</MenuItem>
              <MenuItem value="1002">1002 — Créditos, descuentos y notas</MenuItem>
            </Select>
          </FormControl>
        <TextField
          label="Año gravable"
          value={anioGravable}
          onChange={(e) => setAnioGravable(e.target.value)}
          inputProps={{ inputMode: 'numeric' }}
        />
      </Box>

      <Box sx={{ display: 'flex', gap: 2 }}>
        <Button variant="contained" disabled={!archivo || cargando || empresaId == null} onClick={onCargar}>
          {cargando ? 'Cargando…' : 'Cargar archivo'}
        </Button>
        {resultado && !error && (
          <Button variant="outlined" onClick={() => navigate(`/validacion/${resultado.medioMagneticoId}`)}>
            Ir a validación
          </Button>
        )}
      </Box>

      {!cargandoEmpresa && empresaId == null && (
        <Alert severity="warning">
          No tiene una empresa asignada. Contacte al administrador para vincular su cuenta a una empresa.
        </Alert>
      )}

      {resultado && !error && (
        <Alert severity="success">
          {resultado.registrosCargados} registros cargados correctamente en el año {anioGravable}.
        </Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}
    </Box>
  )
}