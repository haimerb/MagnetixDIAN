import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Paper from '@mui/material/Paper'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import { api } from '../api/client'
import type { CargaResultado } from '../api/client'

export default function CargaPage() {
  const navigate = useNavigate()
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

  const onCargar = async () => {
    if (!archivo) return
    setCargando(true)
    setError(null)
    try {
      const form = new FormData()
      form.append('formato', formato)
      form.append('anioGravable', anioGravable)
      form.append('archivo', archivo)
      const { data } = await api.post<CargaResultado>('/mediomagnetico/1/cargar', form, {
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
        Suba el archivo Excel con las operaciones del formato 1001 (pagos/abonos y retenciones) en la
        plantilla del prevalidador DIAN. Las recargas reemplazan la información existente.
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
        <TextField label="Formato" value={formato} onChange={(e) => setFormato(e.target.value)} />
        <TextField
          label="Año gravable"
          value={anioGravable}
          onChange={(e) => setAnioGravable(e.target.value)}
          inputProps={{ inputMode: 'numeric' }}
        />
      </Box>

      <Box sx={{ display: 'flex', gap: 2 }}>
        <Button variant="contained" disabled={!archivo || cargando} onClick={onCargar}>
          {cargando ? 'Cargando…' : 'Cargar archivo'}
        </Button>
        {resultado && !error && (
          <Button variant="outlined" onClick={() => navigate(`/validacion/${resultado.medioMagneticoId}`)}>
            Ir a validación
          </Button>
        )}
      </Box>

      {resultado && !error && (
        <Alert severity="success">
          {resultado.registrosCargados} registros cargados correctamente en el año {anioGravable}.
        </Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}
    </Box>
  )
}