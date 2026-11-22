import { useCallback, useMemo, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import LinearProgress from '@mui/material/LinearProgress'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import { confirmarImportacion, listarMapeos, vistaPreviaArchivo } from '../api/client'
import type { MapeoColumna, ResultadoImportacionUniversal, VistaPreviaArchivo } from '../api/client'
import EmptyState from '../components/ui/EmptyState'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'

const CAMPOS_DESTINO = [
  'fecha',
  'tipo_documento',
  'numero_documento',
  'nit',
  'razon_social',
  'descripcion',
  'cantidad',
  'valor_unitario',
  'descuento',
  'impuesto',
  'retencion',
  'total',
  'cuenta_contable',
  'referencia',
]

function inferirCampo(cabecera: string): string {
  const nombre = cabecera.toLowerCase().replace(/[\s-]+/g, '_')
  const coincidencia = CAMPOS_DESTINO.find((campo) => nombre.includes(campo.replace(/_/g, ' ')) || nombre.includes(campo))
  return coincidencia ?? 'descripcion'
}

export default function ImportacionUniversalPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [archivo, setArchivo] = useState<File | null>(null)
  const [vistaPrevia, setVistaPrevia] = useState<VistaPreviaArchivo | null>(null)
  const [mapeos, setMapeos] = useState<MapeoColumna[]>([])
  const [resultado, setResultado] = useState<ResultadoImportacionUniversal | null>(null)
  const [analizando, setAnalizando] = useState(false)
  const [importando, setImportando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const onDrop = useCallback((archivos: File[]) => {
    setArchivo(archivos[0] ?? null)
    setVistaPrevia(null)
    setMapeos([])
    setResultado(null)
    setError(null)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: {
      'text/csv': ['.csv'],
      'application/vnd.ms-excel': ['.xls'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'application/json': ['.json'],
    },
    maxFiles: 1,
    disabled: analizando || importando,
  })

  const filas = useMemo(() => vistaPrevia?.filasEjemplo ?? [], [vistaPrevia])
  const confianza = useMemo(() => vistaPrevia?.confianza ?? 0, [vistaPrevia])

  const onVistaPrevia = async () => {
    if (empresaId == null || !archivo) return
    setAnalizando(true)
    setError(null)
    try {
      const respuesta = await vistaPreviaArchivo(empresaId, archivo)
      setVistaPrevia(respuesta)
      setMapeos(
        respuesta.sugerencias?.length
          ? respuesta.sugerencias.map((sugerencia) => ({ ...sugerencia }))
          : respuesta.cabeceras.map((cabecera, indice) => ({
              campo: inferirCampo(cabecera),
              cabeceraOrigen: cabecera,
              indice,
              confianza: 0,
              formatoDetectado: respuesta.formatoDetectado,
            })),
      )
    } finally {
      setAnalizando(false)
    }
  }

  const onConfirmar = async () => {
    if (empresaId == null || !vistaPrevia || !archivo) return
    setImportando(true)
    setError(null)
    try {
      setResultado(
        await confirmarImportacion(empresaId, {
          archivoId: vistaPrevia.archivoId,
          nombreArchivo: vistaPrevia.nombreArchivo || archivo.name,
          formatoDetectado: vistaPrevia.formatoDetectado,
          contenidoBase64: vistaPrevia.contenidoBase64,
          columnas: mapeos.map((mapeo) => ({
            campo: mapeo.campo,
            cabeceraOrigen: mapeo.cabeceraOrigen,
            indice: mapeo.indice,
            confianza: mapeo.confianza,
            formatoDetectado: mapeo.formatoDetectado || vistaPrevia.formatoDetectado,
          })),
        }),
      )
      notificar('Importación confirmada.', 'success')
    } finally {
      setImportando(false)
    }
  }

  const onMapeosGuardados = async () => {
    if (empresaId == null) return
    setError(null)
    try {
      const guardados = await listarMapeos(empresaId)
      if (guardados.length > 0) setMapeos(guardados)
      else notificar('No hay mapeos guardados para esta empresa.', 'info')
    } catch {
      setError('No se pudieron cargar los mapeos guardados.')
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Importaciones"
        title="Importación universal"
        subtitle="Detecte el formato, revise el mapeo de columnas y confirme la carga de forma guiada."
        actions={
          <Button variant="outlined" disabled={empresaId == null} onClick={onMapeosGuardados}>
            Cargar mapeos guardados
          </Button>
        }
      />

      <SectionCard title="Archivo de origen" subtitle="CSV, Excel o JSON">
        <Box
          {...getRootProps({
            role: 'button',
            tabIndex: analizando || importando ? -1 : 0,
            'aria-label': 'Seleccionar archivo para importación universal',
            onKeyDown: (event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                const input = event.currentTarget.querySelector('input[type="file"]')
                if (input instanceof HTMLInputElement) input.click()
              }
            },
          })}
          sx={{
            p: { xs: 3, md: 5 },
            display: 'grid',
            gap: 1.5,
            justifyItems: 'center',
            textAlign: 'center',
            cursor: 'pointer',
            borderRadius: 2,
            border: '1.5px dashed',
            borderColor: isDragActive ? 'primary.main' : (t) => t.palette.divider,
            bgcolor: isDragActive ? (t) => t.palette.primary.main : (t) => t.palette.action.hover,
            color: isDragActive ? 'primary.contrastText' : 'text.secondary',
            '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2 },
          }}
        >
          <input {...getInputProps()} aria-label="Archivo de importación universal" />
          <Box sx={{ display: 'flex', '& svg': { fontSize: 40 } }}>
            {archivo ? <InsertDriveFileOutlinedIcon /> : <CloudUploadOutlinedIcon />}
          </Box>
          <Typography>{archivo ? archivo.name : 'Arrastre el archivo aquí o haga clic para seleccionarlo'}</Typography>
        </Box>

        <Stack direction="row" spacing={2} sx={{ mt: 3 }}>
          <Button
            variant="contained"
            disabled={!archivo || empresaId == null || analizando}
            loading={analizando}
            onClick={onVistaPrevia}
          >
            Analizar archivo
          </Button>
          <Button
            variant="outlined"
            disabled={!vistaPrevia || empresaId == null || importando}
            loading={importando}
            onClick={onConfirmar}
          >
            Confirmar importación
          </Button>
        </Stack>
        {analizando && <LinearProgress sx={{ mt: 2 }} />}
      </SectionCard>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para importar información.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      {vistaPrevia && (
        <>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <StatCard label="Formato detectado" value={vistaPrevia.formatoDetectado || '—'} icon={<InsertDriveFileOutlinedIcon />} />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <StatCard label="Columnas" value={vistaPrevia.cabeceras.length} tone="info" />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <StatCard label="Confianza" value={`${Math.round(confianza * 100)}%`} tone="success" icon={<CheckCircleOutlineIcon />} />
            </Grid>
          </Grid>

          <SectionCard title="Mapeo de columnas" subtitle="Ajuste el campo destino antes de confirmar" padded={false}>
            {mapeos.length === 0 ? (
              <EmptyState icon={<CheckCircleOutlineIcon />} title="Sin mapeos detectados" />
            ) : (
              <TableContainer>
                  <Table size="small" aria-label="Mapeo de columnas" sx={{ minWidth: 720 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Índice</TableCell>
                      <TableCell>Cabecera origen</TableCell>
                      <TableCell>Campo destino</TableCell>
                      <TableCell>Formato detectado</TableCell>
                      <TableCell align="right">Confianza</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {mapeos.map((mapeo) => (
                      <TableRow key={`${mapeo.indice}-${mapeo.cabeceraOrigen}`}>
                        <TableCell>{mapeo.indice}</TableCell>
                        <TableCell>{mapeo.cabeceraOrigen}</TableCell>
                        <TableCell>
                          <TextField
                            select
                            size="small"
                            value={mapeo.campo}
                            onChange={(event) =>
                              setMapeos((actuales) =>
                                actuales.map((actual) =>
                                  actual.indice === mapeo.indice
                                    ? { ...actual, campo: event.target.value }
                                    : actual,
                                ),
                              )
                            }
                            sx={{ minWidth: 220 }}
                          >
                            {CAMPOS_DESTINO.map((campo) => (
                              <MenuItem key={campo} value={campo}>
                                {campo}
                              </MenuItem>
                            ))}
                          </TextField>
                        </TableCell>
                        <TableCell>{mapeo.formatoDetectado}</TableCell>
                        <TableCell align="right">{Math.round(mapeo.confianza * 100)}%</TableCell>
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </SectionCard>

          <SectionCard title="Vista previa" subtitle="Primeras filas del archivo" padded={false}>
            {filas.length === 0 ? (
              <EmptyState icon={<InsertDriveFileOutlinedIcon />} title="Sin filas para mostrar" />
            ) : (
              <TableContainer>
                <Table size="small" aria-label="Vista previa del archivo" sx={{ minWidth: 640 }}>
                  <TableHead>
                    <TableRow>
                        {vistaPrevia.cabeceras.map((cabecera) => (
                          <TableCell key={cabecera}>{cabecera}</TableCell>
                        ))}
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {filas.slice(0, 5).map((fila) => (
                      <TableRow key={fila.join('|')}>
                        {fila.map((celda, posicion) => (
                          <TableCell key={`${vistaPrevia.cabeceras[posicion] ?? 'columna'}-${celda}`}>
                            {celda}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </SectionCard>
        </>
      )}

      {resultado && (
        <Alert
          severity={resultado.filasImportadas > 0 ? 'success' : 'warning'}
          action={
            <Button color="inherit" size="small" onClick={() => setResultado(null)}>
              Cerrar
            </Button>
          }
        >
          {resultado.filasLeidas} fila(s) leídas, {resultado.filasImportadas} importadas,{' '}
          {resultado.filasOmitidas} omitidas y {resultado.errores} error(es).
          {resultado.mensajes.length > 0 ? ` ${resultado.mensajes.join(' ')}` : ''}
        </Alert>
      )}
    </Box>
  )
}
