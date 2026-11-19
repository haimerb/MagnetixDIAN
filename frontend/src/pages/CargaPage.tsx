import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useDropzone } from 'react-dropzone'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import LinearProgress from '@mui/material/LinearProgress'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import { api } from '../api/client'
import type { CargaResultado } from '../api/client'
import { descargarArchivo } from '../api/descargas'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'

export default function CargaPage() {
  const navigate = useNavigate()
  const { notificar } = useNotificaciones()
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [archivo, setArchivo] = useState<File | null>(null)
  const [anioGravable, setAnioGravable] = useState('2025')
  const [formato, setFormato] = useState('1001')
  const [cargando, setCargando] = useState(false)
  const [descargandoPlantilla, setDescargandoPlantilla] = useState(false)
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
    disabled: cargando,
  })

  const onDescargarPlantilla = async () => {
    setDescargandoPlantilla(true)
    try {
      await descargarArchivo(`/api/mediomagnetico/plantilla?formato=${formato}`, `plantilla-${formato}.xlsx`)
    } catch (e) {
      notificar(e instanceof Error ? e.message : 'No se pudo descargar la plantilla.', 'error')
    } finally {
      setDescargandoPlantilla(false)
    }
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
    <Box component="section" sx={{ maxWidth: 760, display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Paso 1 de 3"
        title="Carga de datos"
        subtitle={`Suba el archivo Excel con las operaciones del formato ${formato} en la plantilla del prevalidador DIAN. Las recargas reemplazan la información existente.`}
        actions={
          <Button variant="outlined" onClick={onDescargarPlantilla} loading={descargandoPlantilla}>
            Descargar plantilla (.xlsx)
          </Button>
        }
      />

      <SectionCard title="Archivo de operaciones">
        <Box
          {...getRootProps()}
          sx={{
            p: { xs: 3, md: 5 },
            cursor: 'pointer',
            textAlign: 'center',
            display: 'grid',
            gap: 1.5,
            justifyItems: 'center',
            borderRadius: 2,
            border: '1.5px dashed',
            borderColor: isDragActive ? 'primary.main' : (t) => t.palette.divider,
            bgcolor: isDragActive ? (t) => t.palette.primary.main : (t) => t.palette.action.hover,
            color: isDragActive ? 'primary.contrastText' : 'text.secondary',
            opacity: cargando ? 0.6 : 1,
            pointerEvents: cargando ? 'none' : undefined,
            transition: 'border-color 180ms ease, background-color 180ms ease',
          }}
        >
          <input {...getInputProps()} aria-label="Archivo Excel a cargar" />
          <Box sx={{ display: 'flex', '& svg': { fontSize: 40 } }}>
            {archivo ? <InsertDriveFileOutlinedIcon /> : <CloudUploadOutlinedIcon />}
          </Box>
          <Typography variant="body1" sx={{ color: 'inherit' }}>
            {archivo
              ? `Archivo seleccionado: ${archivo.name}`
              : isDragActive
                ? 'Suelte el archivo aquí'
                : 'Arrastre el .xlsx aquí o haga clic para seleccionar'}
          </Typography>
          {!archivo && (
            <Typography variant="caption" sx={{ color: 'inherit' }}>
              Formato aceptado: .xlsx
            </Typography>
          )}
        </Box>

        <Box
          sx={{
            mt: 3,
            display: 'grid',
            gap: 2,
            gridTemplateColumns: { xs: '1fr', md: 'repeat(2, 1fr)' },
          }}
        >
          <FormControl>
            <InputLabel id="formato-label">Formato</InputLabel>
            <Select
              labelId="formato-label"
              label="Formato"
              value={formato}
              onChange={(e) => setFormato(e.target.value)}
            >
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

        <Box sx={{ display: 'flex', gap: 2, mt: 3, alignItems: 'center' }}>
          <Button
            variant="contained"
            disabled={!archivo || empresaId == null}
            loading={cargando}
            onClick={onCargar}
          >
            Cargar archivo
          </Button>
          {resultado && !error && (
            <Button variant="outlined" onClick={() => navigate(`/validacion/${resultado.medioMagneticoId}`)}>
              Ir a validación
            </Button>
          )}
        </Box>

        {cargando && <LinearProgress sx={{ mt: 3 }} />}
      </SectionCard>

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
