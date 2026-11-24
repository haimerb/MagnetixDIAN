import { useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Checkbox from '@mui/material/Checkbox'
import FormControlLabel from '@mui/material/FormControlLabel'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import type { ResultadoGenerarTodoExogena } from '../../api/client'
import { descargarTexto } from '../../api/descargas'
import {
  extensionDescarga,
  FORMATO_EXOGENA_PRINCIPAL,
  generarTodoExogena,
  listarFormatosExogena,
} from '../../api/erp/exogena'
import EmptyState from '../../components/ui/EmptyState'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import { useEmpresaActiva } from '../../features/auth/useEmpresaActiva'
import { useRecursoEmpresa } from '../../features/erp/useRecursoEmpresa'
import { useNotificaciones } from '../../features/notificaciones/contexto'

function fechaHoy(): string {
  return new Date().toISOString().slice(0, 10)
}

export default function ExogenaPage() {
  const { empresaId } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const formatos = useRecursoEmpresa(empresaId, listarFormatosExogena)
  const [anio, setAnio] = useState(() => new Date().getFullYear())
  const [formato, setFormato] = useState(FORMATO_EXOGENA_PRINCIPAL)
  const [incluirTodos, setIncluirTodos] = useState(false)
  const [resultado, setResultado] = useState<ResultadoGenerarTodoExogena | null>(null)
  const [generando, setGenerando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const generar = async () => {
    if (empresaId == null) return
    setGenerando(true)
    setError(null)
    try {
      const respuesta = await generarTodoExogena(empresaId, {
        anioGravable: anio,
        numeroEnvio: 1,
        fechaEnvio: fechaHoy(),
        fechaInicial: `${anio}-01-01`,
        fechaFinal: `${anio}-12-31`,
        formatos: incluirTodos ? formatos.datos.map((item) => item.codigo) : [formato],
      })
      setResultado(respuesta)
      notificar('Exógena generada correctamente.', 'success')
    } catch {
      setError('No se pudo generar la información exógena.')
    } finally {
      setGenerando(false)
    }
  }

  const descargar = (xml: string, excel: string | null, codigo: string) => {
    const archivo = extensionDescarga(xml, excel)
    descargarTexto(archivo.contenido, `${codigo}-${archivo.nombre}`, archivo.mime)
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Contabilidad"
        title="Información exógena"
        subtitle="Genere los formatos de la DIAN desde la facturación y la contabilidad de la empresa."
      />

      <Alert severity="info">Prevalidación oficial DIAN: no disponible en este módulo.</Alert>
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard title="Generar exógena" subtitle="Parámetros del año gravable y formato de salida.">
        <Grid container spacing={2}>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              label="Año"
              type="number"
              value={anio}
              onChange={(evento) => setAnio(Number(evento.target.value))}
              fullWidth
            />
          </Grid>
          <Grid size={{ xs: 12, md: 4 }}>
            <TextField
              select
              label="Formato"
              value={formato}
              onChange={(evento) => setFormato(evento.target.value)}
              disabled={incluirTodos}
              fullWidth
            >
              <MenuItem value={FORMATO_EXOGENA_PRINCIPAL}>{FORMATO_EXOGENA_PRINCIPAL} · Principal</MenuItem>
              {formatos.datos
                .filter((item) => item.codigo !== FORMATO_EXOGENA_PRINCIPAL)
                .map((item) => (
                  <MenuItem key={item.codigo} value={item.codigo}>
                    {item.codigo} · {item.descripcion}
                  </MenuItem>
                ))}
            </TextField>
          </Grid>
          <Grid size={{ xs: 12, md: 4 }} sx={{ display: 'flex', alignItems: 'center' }}>
            <FormControlLabel
              control={
                <Checkbox
                  checked={incluirTodos}
                  onChange={(evento) => setIncluirTodos(evento.target.checked)}
                />
              }
              label="Generar todos los formatos"
            />
          </Grid>
        </Grid>
        <Box sx={{ mt: 3 }}>
          <Button variant="contained" loading={generando} disabled={empresaId == null} onClick={() => void generar()}>
            Generar Todo
          </Button>
        </Box>
      </SectionCard>

      <SectionCard title="Resultados">
        {resultado == null ? (
          <EmptyState
            title="Aún no hay resultados"
            description="Seleccione el año y el formato, luego presione Generar Todo."
          />
        ) : (
          <Stack spacing={2}>
            <Alert severity="success">
              {resultado.totalFormatos} formato(s) generado(s) para el año {resultado.anioGravable}.
            </Alert>
            {resultado.resultados.map((item) => (
              <Box
                key={item.formato}
                sx={{ display: 'grid', gap: 1, p: 2, border: '1px solid', borderColor: 'divider', borderRadius: 2 }}
              >
                <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
                  <CheckCircleOutlineIcon color="success" fontSize="small" />
                  <Typography variant="subtitle1">{item.formato} · {item.descripcion}</Typography>
                </Stack>
                <Typography variant="body2" color="text.secondary">
                  {item.registros} registros · validación: {item.validacion}
                </Typography>
                {item.errores.length > 0 && (
                  <Alert severity="warning">
                    <Box component="ul" sx={{ m: 0, pl: 2 }}>
                      {item.errores.map((detalle) => (
                        <li key={`${detalle.campo}-${detalle.mensaje}`}>
                          {detalle.campo}: {detalle.mensaje}
                        </li>
                      ))}
                    </Box>
                  </Alert>
                )}
                <Button
                  size="small"
                  variant="outlined"
                  onClick={() => descargar(item.xml, item.excel, item.formato)}
                >
                  Descargar {item.excel ? 'Excel' : 'XML'}
                </Button>
              </Box>
            ))}
          </Stack>
        )}
      </SectionCard>
    </Box>
  )
}
