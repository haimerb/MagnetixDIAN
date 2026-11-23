import { useCallback, useEffect, useRef, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import FormControl from '@mui/material/FormControl'
import Grid from '@mui/material/Grid2'
import IconButton from '@mui/material/IconButton'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import TextField from '@mui/material/TextField'
import AddOutlinedIcon from '@mui/icons-material/AddOutlined'
import DeleteOutlineOutlinedIcon from '@mui/icons-material/DeleteOutlineOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import VerifiedOutlinedIcon from '@mui/icons-material/VerifiedOutlined'
import {
  generarExogena,
  listarConceptosExogena,
  listarFormatosExogena,
  validarXmlExogena,
  type ConceptoExogena,
  type FormatoExogena,
  type ResultadoGeneracionExogena,
  type ResultadoValidacionExogena,
} from '../../api/client'
import { descargarTexto } from '../../api/descargas'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'

type ExogenaTabProps = {
  empresaId: number | null
  cargandoEmpresa: boolean
}

type FilaExogena = {
  id: number
  valores: Record<string, string>
}

function hoy() {
  return new Date().toISOString().slice(0, 10)
}

export default function ExogenaTab({ empresaId, cargandoEmpresa }: ExogenaTabProps) {
  const siguienteId = useRef(1)
  const [formatos, setFormatos] = useState<FormatoExogena[]>([])
  const [formato, setFormato] = useState('')
  const [conceptos, setConceptos] = useState<ConceptoExogena[]>([])
  const [filas, setFilas] = useState<FilaExogena[]>([])
  const [anioGravable, setAnioGravable] = useState(String(new Date().getFullYear()))
  const [numeroEnvio, setNumeroEnvio] = useState('1')
  const [fechaEnvio, setFechaEnvio] = useState(hoy)
  const [fechaInicial, setFechaInicial] = useState(hoy)
  const [fechaFinal, setFechaFinal] = useState(hoy)
  const [concepto, setConcepto] = useState('')
  const [cargandoFormatos, setCargandoFormatos] = useState(true)
  const [cargandoConceptos, setCargandoConceptos] = useState(false)
  const [generando, setGenerando] = useState(false)
  const [validando, setValidando] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [resultado, setResultado] = useState<ResultadoGeneracionExogena | null>(null)
  const [validacion, setValidacion] = useState<ResultadoValidacionExogena | null>(null)

  const crearFila = useCallback(
    (encabezados: string[]): FilaExogena => ({
      id: siguienteId.current++,
      valores: Object.fromEntries(encabezados.map((encabezado) => [encabezado, ''])),
    }),
    [],
  )

  const seleccionarFormato = useCallback(
    (item: FormatoExogena) => {
      setFormato(item.codigo)
      setFilas([crearFila(item.encabezados)])
      setResultado(null)
      setValidacion(null)
      setConcepto('')
      if (empresaId != null) {
        setCargandoConceptos(true)
        listarConceptosExogena(empresaId, item.codigo)
          .then((datos) => {
            setConceptos(datos)
            setConcepto(datos[0]?.codigo ?? '')
          })
          .catch(() => setError('No se pudieron cargar los conceptos del formato.'))
          .finally(() => setCargandoConceptos(false))
      }
    },
    [crearFila, empresaId],
  )

  useEffect(() => {
    if (empresaId == null) {
      setCargandoFormatos(false)
      return
    }
    setCargandoFormatos(true)
    listarFormatosExogena(empresaId)
      .then((datos) => {
        setFormatos(datos)
        const primero = datos.find((item) => item.activo) ?? datos[0]
        if (primero) seleccionarFormato(primero)
      })
      .catch(() => setError('No se pudieron cargar los formatos exógenos.'))
      .finally(() => setCargandoFormatos(false))
  }, [empresaId, seleccionarFormato])

  const formatoActual = formatos.find((item) => item.codigo === formato) ?? null
  const encabezados = formatoActual?.encabezados ?? []

  const actualizarCelda = (id: number, encabezado: string, valor: string) => {
    setFilas((anteriores) =>
      anteriores.map((fila) =>
        fila.id === id ? { ...fila, valores: { ...fila.valores, [encabezado]: valor } } : fila,
      ),
    )
  }

  const generar = async () => {
    if (empresaId == null || !formato) return
    setGenerando(true)
    setError(null)
    setResultado(null)
    setValidacion(null)
    try {
      setResultado(
        await generarExogena(empresaId, formato, {
          formato,
          anioGravable: Number(anioGravable),
          numeroEnvio: Number(numeroEnvio),
          fechaEnvio,
          fechaInicial,
          fechaFinal,
          concepto,
          encabezados,
          filas: filas.map((fila) => fila.valores),
        }),
      )
    } catch {
      setError('No se pudo generar el XML exógeno. Revise los datos e intente de nuevo.')
    } finally {
      setGenerando(false)
    }
  }

  const validar = async () => {
    if (empresaId == null || !formato || !resultado?.xml) return
    setValidando(true)
    try {
      setValidacion(await validarXmlExogena(empresaId, formato, resultado.xml))
    } catch {
      setError('No se pudo validar el XML contra el XSD local.')
    } finally {
      setValidando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {error && <Alert severity="error">{error}</Alert>}
      <Alert severity="warning">
        La validación disponible es XSD local y no equivale al prevalidador oficial de la DIAN.
      </Alert>

      {cargandoFormatos || cargandoEmpresa ? (
        <SectionCard title="Formatos oficiales">
          <EmptyState title="Cargando formatos exógenos…" />
        </SectionCard>
      ) : formatos.length === 0 ? (
        <SectionCard title="Formatos oficiales">
          <EmptyState
            icon={<DescriptionOutlinedIcon />}
            title="Sin formatos disponibles"
            description="No hay formatos exógenos activos para la empresa."
          />
        </SectionCard>
      ) : (
        <>
          <SectionCard title="Formato y periodo" subtitle="Formatos oficiales 1125, 2278 y 5253">
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 4 }}>
                <FormControl fullWidth size="small">
                  <InputLabel id="formato-exogena-label">Formato</InputLabel>
                  <Select
                    labelId="formato-exogena-label"
                    label="Formato"
                    value={formato}
                    onChange={(event) => {
                      const item = formatos.find((formatoItem) => formatoItem.codigo === event.target.value)
                      if (item) seleccionarFormato(item)
                    }}
                  >
                    {formatos.map((item) => (
                      <MenuItem key={item.codigo} value={item.codigo}>
                        {item.codigo} · {item.descripcion}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 12, md: 4 }}>
                <FormControl fullWidth size="small" disabled={cargandoConceptos || conceptos.length === 0}>
                  <InputLabel id="concepto-exogena-label">Concepto</InputLabel>
                  <Select
                    labelId="concepto-exogena-label"
                    label="Concepto"
                    value={concepto}
                    onChange={(event) => setConcepto(String(event.target.value))}
                  >
                    {conceptos.map((item) => (
                      <MenuItem key={`${item.formato}-${item.codigo}`} value={item.codigo}>
                        {item.codigo} · {item.descripcion}
                      </MenuItem>
                    ))}
                  </Select>
                </FormControl>
              </Grid>
              <Grid size={{ xs: 6, md: 2 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  label="Año gravable"
                  value={anioGravable}
                  onChange={(event) => setAnioGravable(event.target.value)}
                />
              </Grid>
              <Grid size={{ xs: 6, md: 2 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="number"
                  label="N.º envío"
                  value={numeroEnvio}
                  onChange={(event) => setNumeroEnvio(event.target.value)}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="date"
                  label="Fecha envío"
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={fechaEnvio}
                  onChange={(event) => setFechaEnvio(event.target.value)}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="date"
                  label="Fecha inicial"
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={fechaInicial}
                  onChange={(event) => setFechaInicial(event.target.value)}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 3 }}>
                <TextField
                  fullWidth
                  size="small"
                  type="date"
                  label="Fecha final"
                  slotProps={{ inputLabel: { shrink: true } }}
                  value={fechaFinal}
                  onChange={(event) => setFechaFinal(event.target.value)}
                />
              </Grid>
            </Grid>
          </SectionCard>

          <SectionCard
            title="Registros del formato"
            subtitle={`${filas.length} fila(s) · ${encabezados.length} campo(s)`}
            actions={
              <Button
                size="small"
                startIcon={<AddOutlinedIcon />}
                onClick={() => setFilas((anteriores) => [...anteriores, crearFila(encabezados)])}
              >
                Agregar fila
              </Button>
            }
          >
            <Box component="form" onSubmit={(event) => { event.preventDefault(); generar() }} sx={{ display: 'grid', gap: 2 }}>
              {filas.map((fila, indiceFila) => (
                <Box
                  key={fila.id}
                  sx={{ border: 1, borderColor: 'divider', borderRadius: 2, p: 2, display: 'grid', gap: 2 }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                    <strong>Fila {indiceFila + 1}</strong>
                    <IconButton
                      size="small"
                      aria-label={`Eliminar fila ${indiceFila + 1}`}
                      disabled={filas.length === 1}
                      onClick={() => setFilas((anteriores) => anteriores.filter((item) => item.id !== fila.id))}
                    >
                      <DeleteOutlineOutlinedIcon />
                    </IconButton>
                  </Box>
                  <Grid container spacing={2}>
                    {encabezados.map((encabezado) => (
                      <Grid key={encabezado} size={{ xs: 12, sm: 6, md: 4 }}>
                        <TextField
                          fullWidth
                          size="small"
                          label={encabezado}
                          value={fila.valores[encabezado] ?? ''}
                          onChange={(event) => actualizarCelda(fila.id, encabezado, event.target.value)}
                        />
                      </Grid>
                    ))}
                  </Grid>
                </Box>
              ))}
              <Box>
                <Button type="submit" variant="contained" loading={generando} disabled={empresaId == null}>
                  Generar XML
                </Button>
              </Box>
            </Box>
          </SectionCard>

          {resultado && (
            <SectionCard
              title="XML generado"
              subtitle={`${resultado.formato} · ${resultado.version ?? 'sin versión'} · ${resultado.registros} registros`}
              actions={
                <>
                  <Button
                    size="small"
                    startIcon={<VerifiedOutlinedIcon />}
                    loading={validando}
                    onClick={validar}
                  >
                    Validar XSD
                  </Button>
                  <Button
                    size="small"
                    startIcon={<DownloadOutlinedIcon />}
                    onClick={() => descargarTexto(resultado.xml, `exogena-${resultado.formato}.xml`)}
                  >
                    Descargar
                  </Button>
                </>
              }
            >
              <Box
                component="pre"
                sx={{
                  m: 0,
                  p: 2,
                  maxHeight: 320,
                  overflow: 'auto',
                  borderRadius: 2,
                  bgcolor: 'action.hover',
                  fontSize: '0.75rem',
                }}
              >
                {resultado.xml}
              </Box>
              {validacion && (
                <Alert severity={validacion.valido ? 'success' : 'error'} sx={{ mt: 2 }}>
                  {validacion.mensaje}
                </Alert>
              )}
            </SectionCard>
          )}
        </>
      )}
    </Box>
  )
}
