import { useCallback, useState } from 'react'
import { useDropzone } from 'react-dropzone'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import LinearProgress from '@mui/material/LinearProgress'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import Tooltip from '@mui/material/Tooltip'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import CloudUploadOutlinedIcon from '@mui/icons-material/CloudUploadOutlined'
import InsertDriveFileOutlinedIcon from '@mui/icons-material/InsertDriveFileOutlined'
import RemoveCircleOutlineIcon from '@mui/icons-material/RemoveCircleOutline'
import { importarMultiformato, validarDatosJson } from '../api/client'
import type { ImportacionMultiFormato, ValidacionEstructura } from '../api/client'
import EstadoChip from '../components/ui/EstadoChip'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'

export default function ImportacionAvanzadaPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const [archivo, setArchivo] = useState<File | null>(null)
  const [anioGravable, setAnioGravable] = useState(() => String(new Date().getFullYear() - 1))
  const [cargando, setCargando] = useState(false)
  const [resultado, setResultado] = useState<ImportacionMultiFormato | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [validando, setValidando] = useState<number | null>(null)
  const [validacion, setValidacion] = useState<ValidacionEstructura | null>(null)

  const anio = Number(anioGravable)
  const anioValido = Number.isInteger(anio) && anio >= 1900 && anio <= 2100

  const onDrop = useCallback((files: File[]) => {
    setArchivo(files[0] ?? null)
    setResultado(null)
    setError(null)
    setValidacion(null)
  }, [])

  const { getRootProps, getInputProps, isDragActive } = useDropzone({
    onDrop,
    accept: { 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'] },
    maxFiles: 1,
    disabled: cargando,
  })

  const onImportar = async () => {
    if (!archivo || empresaId == null || !anioValido) return
    setCargando(true)
    setError(null)
    try {
      setResultado(await importarMultiformato(empresaId, anio, archivo))
    } catch {
      setError('No se pudo analizar el workbook. Verifique que sea un Excel (.xlsx) válido.')
    } finally {
      setCargando(false)
    }
  }

  const onValidar = async (medioId: number) => {
    setValidando(medioId)
    setError(null)
    try {
      setValidacion(await validarDatosJson(medioId))
    } catch {
      setError('No se pudo validar la estructura del registro persistido.')
    } finally {
      setValidando(null)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Modo inteligente"
        title="Importación avanzada"
        subtitle="Analice un workbook con varias hojas de formatos exógenos. Los formatos activos se guardan como borradores; los demás se muestran solo como vista previa."
      />

      <SectionCard
        title="Workbook de exógena"
        subtitle="El código del formato se detecta en el nombre o encabezado de cada hoja."
      >
        <Box
          {...getRootProps({
            role: 'button',
            tabIndex: cargando ? -1 : 0,
            'aria-label': 'Seleccionar archivo Excel multiformato',
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
            '&:focus-visible': { outline: '2px solid', outlineColor: 'primary.main', outlineOffset: 2 },
          }}
        >
          <input {...getInputProps()} aria-label="Archivo Excel multiformato" />
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
          <TextField
            label="Año gravable"
            value={anioGravable}
            onChange={(event) => setAnioGravable(event.target.value)}
            error={!anioValido}
            helperText={anioValido ? undefined : 'Ingrese un año entre 1900 y 2100.'}
            inputProps={{ inputMode: 'numeric' }}
          />
        </Box>

        <Box sx={{ display: 'flex', gap: 2, mt: 3, alignItems: 'center' }}>
          <Button
            variant="contained"
            disabled={!archivo || empresaId == null || !anioValido || cargando}
            loading={cargando}
            onClick={onImportar}
          >
            Analizar workbook
          </Button>
        </Box>

        {cargando && <LinearProgress sx={{ mt: 3 }} />}
      </SectionCard>

      {!cargandoEmpresa && empresaId == null && (
        <Alert severity="warning">
          No tiene una empresa asignada. Contacte al administrador para vincular su cuenta a una empresa.
        </Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      {resultado && (
        <>
          <Alert severity="info">
            {resultado.formatosPersistidos} formatos activos guardados en el año {resultado.anioGravable}.
            Los formatos inactivos no generan XML ni pueden presentarse.
          </Alert>

          {resultado.hojasOmitidas.length > 0 && (
            <Alert severity="warning">
              Hojas sin código reconocido: {resultado.hojasOmitidas.join(', ')}.
            </Alert>
          )}

          <SectionCard
            title="Formatos detectados"
            subtitle={`${resultado.formatos.length} hojas con código de formato`}
          >
            <TableContainer>
              <Table size="small">
                <TableHead>
                  <TableRow>
                    <TableCell>Hoja</TableCell>
                    <TableCell>Formato</TableCell>
                    <TableCell>Estado</TableCell>
                    <TableCell align="right">Filas</TableCell>
                    <TableCell align="center">Persistido</TableCell>
                    <TableCell align="center">Estructura</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {resultado.formatos.map((formato) => (
                    <TableRow key={`${formato.codigo}-${formato.hoja}`} hover>
                      <TableCell>{formato.hoja}</TableCell>
                      <TableCell>{formato.codigo}</TableCell>
                      <TableCell>
                        <EstadoChip estado={formato.estado} />
                      </TableCell>
                      <TableCell align="right">{formato.filas}</TableCell>
                      <TableCell align="center">
                        <Tooltip
                          title={
                            formato.medioMagneticoId == null
                              ? 'No persistido: formato inactivo o desconocido'
                              : `Registro #${formato.medioMagneticoId}`
                          }
                        >
                          {formato.medioMagneticoId == null ? (
                            <RemoveCircleOutlineIcon color="disabled" aria-label="No persistido" />
                          ) : (
                            <CheckCircleOutlineIcon color="success" aria-label="Persistido" />
                          )}
                        </Tooltip>
                      </TableCell>
                      <TableCell align="center">
                        {formato.medioMagneticoId != null && (
                          <Button
                            size="small"
                            variant="outlined"
                            aria-label={`Validar estructura del registro ${formato.medioMagneticoId}`}
                            disabled={validando !== null}
                            loading={validando === formato.medioMagneticoId}
                            onClick={() => onValidar(formato.medioMagneticoId as number)}
                          >
                            Validar
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          </SectionCard>

          {validacion && (
            <SectionCard
              title={`Validación estructural · registro #${validacion.medioMagneticoId}`}
              subtitle={`${validacion.registrosValidados} filas revisadas · ${validacion.erroresTotal} errores · ${validacion.advertenciasTotal} advertencias`}
            >
              <Alert
                severity={
                  validacion.erroresTotal > 0
                    ? 'error'
                    : validacion.advertenciasTotal > 0
                      ? 'warning'
                      : 'success'
                }
              >
                {validacion.erroresTotal > 0
                  ? 'La estructura del registro no pasó la validación. Revise los detalles antes de continuar.'
                  : 'La estructura del registro es válida. Esto no genera XML ni cambia el estado del medio.'}
              </Alert>

              {validacion.errores.length > 0 && (
                <TableContainer sx={{ mt: 2 }}>
                  <Table size="small">
                    <TableHead>
                      <TableRow>
                        <TableCell>Línea</TableCell>
                        <TableCell>Campo</TableCell>
                        <TableCell>Esperado</TableCell>
                        <TableCell>Encontrado</TableCell>
                        <TableCell>Detalle</TableCell>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {validacion.errores.map((detalle) => (
                        <TableRow
                          key={`${detalle.linea ?? 0}-${detalle.campo ?? ''}-${detalle.valorEsperado ?? ''}-${detalle.mensaje}`}
                          hover
                        >
                          <TableCell>{detalle.linea ?? '-'}</TableCell>
                          <TableCell>{detalle.campo ?? '-'}</TableCell>
                          <TableCell>{detalle.valorEsperado ?? '-'}</TableCell>
                          <TableCell>{detalle.valorEncontrado ?? '-'}</TableCell>
                          <TableCell>{detalle.mensaje}</TableCell>
                        </TableRow>
                      ))}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </SectionCard>
          )}
        </>
      )}
    </Box>
  )
}
