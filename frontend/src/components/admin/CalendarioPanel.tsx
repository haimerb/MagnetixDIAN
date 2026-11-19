import { useEffect, useState } from 'react'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import CircularProgress from '@mui/material/CircularProgress'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import IconButton from '@mui/material/IconButton'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import DeleteIcon from '@mui/icons-material/Delete'
import EventOutlinedIcon from '@mui/icons-material/EventOutlined'
import { api, type CalendarioAdmin } from '../../api/client'
import ConfirmDialog from '../ui/ConfirmDialog'
import EmptyState from '../ui/EmptyState'
import SectionCard from '../ui/SectionCard'
import TableSkeleton from '../ui/TableSkeleton'

const TIPOS_REPORTE = ['GRAN_CONTRIBUYENTE', 'PERSONA_JURIDICA', 'PERSONA_NATURAL']

export default function CalendarioPanel({ onError }: { onError: (mensaje: string) => void }) {
  const [calendario, setCalendario] = useState<CalendarioAdmin[]>([])
  const [anio, setAnio] = useState(2025)
  const [cargando, setCargando] = useState(false)
  const [dialogAbierto, setDialogAbierto] = useState(false)
  const [guardando, setGuardando] = useState(false)
  const [eliminando, setEliminando] = useState<number | null>(null)
  const [confirmacion, setConfirmacion] = useState<number | null>(null)
  const [form, setForm] = useState({ anioPresentacion: 2026, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' })

  const cargar = () => {
    setCargando(true)
    return api
      .get<CalendarioAdmin[]>(`/admin/calendario?anioGravable=${anio}`)
      .then(r => setCalendario(r.data))
      .catch(() => onError('No se pudo cargar el calendario.'))
      .finally(() => setCargando(false))
  }

  useEffect(() => { cargar() }, [anio]) // eslint-disable-line react-hooks/exhaustive-deps

  const crear = async () => {
    setGuardando(true)
    try {
      await api.post('/admin/calendario', {
        anioGravable: anio,
        anioPresentacion: Number(form.anioPresentacion),
        tipoReporte: form.tipoReporte,
        rangoNitIni: form.rangoNitIni || null,
        rangoNitFin: form.rangoNitFin || null,
        fechaLimite: form.fechaLimite,
      }, { skipGlobalError: true })
      setDialogAbierto(false)
      cargar()
    } catch (e: unknown) {
      onError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'Error al crear fecha')
    } finally {
      setGuardando(false)
    }
  }

  const confirmarEliminar = async () => {
    if (confirmacion === null) return
    setEliminando(confirmacion)
    try {
      await api.delete(`/admin/calendario/${confirmacion}`, { skipGlobalError: true })
      cargar()
      setConfirmacion(null)
    } catch (e: unknown) {
      onError((e as { response?: { data?: { message?: string } } }).response?.data?.message ?? 'No se pudo eliminar la fecha')
    } finally {
      setEliminando(null)
    }
  }

  return (
    <>
      <SectionCard
        title="Calendario DIAN"
        actions={
          <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
            <TextField label="Año gravable" type="number" size="small" value={anio} onChange={e => setAnio(Number(e.target.value))} sx={{ width: 140 }} />
            <Button variant="contained" onClick={() => { setDialogAbierto(true); setForm({ anioPresentacion: anio + 1, tipoReporte: 'PERSONA_JURIDICA', rangoNitIni: '', rangoNitFin: '', fechaLimite: '' }) }}>
              Nueva fecha
            </Button>
          </Stack>
        }
        padded={!cargando && calendario.length === 0}
      >
        {cargando ? (
          <TableSkeleton filas={5} columnas={5} label="Cargando calendario…" />
        ) : calendario.length === 0 ? (
          <EmptyState icon={<EventOutlinedIcon />} title="Sin fechas para este año" description="Agregue las fechas límite del calendario tributario." />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Calendario DIAN">
              <TableHead>
                <TableRow>
                  <TableCell>Tipo reporte</TableCell><TableCell>Rango NIT</TableCell><TableCell>Fecha límite</TableCell><TableCell>Año presentación</TableCell><TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {calendario.map(c => (
                  <TableRow key={c.id} hover>
                    <TableCell>{c.tipoReporte.replace('_', ' ')}</TableCell>
                    <TableCell>{c.rangoNitIni ?? '*'} – {c.rangoNitFin ?? '*'}</TableCell>
                    <TableCell>{c.fechaLimite}</TableCell>
                    <TableCell>{c.anioPresentacion}</TableCell>
                    <TableCell align="right">
                      <IconButton size="small" color="error" onClick={() => setConfirmacion(c.id)} aria-label="Eliminar" disabled={eliminando === c.id}>
                        {eliminando === c.id ? <CircularProgress size={16} /> : <DeleteIcon fontSize="small" />}
                      </IconButton>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </SectionCard>

      <Dialog open={dialogAbierto} onClose={() => { if (!guardando) setDialogAbierto(false) }} maxWidth="sm" fullWidth>
        <DialogTitle>Nueva fecha DIAN</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <TextField label="Año presentación" type="number" required value={form.anioPresentacion} onChange={e => setForm(p => ({ ...p, anioPresentacion: Number(e.target.value) }))} />
          <TextField label="Tipo reporte" select required value={form.tipoReporte} onChange={e => setForm(p => ({ ...p, tipoReporte: e.target.value }))}>
            {TIPOS_REPORTE.map(t => <MenuItem key={t} value={t}>{t.replace(/_/g, ' ')}</MenuItem>)}
          </TextField>
          <Box sx={{ display: 'flex', gap: 1 }}>
            <TextField label="Rango NIT inicio" value={form.rangoNitIni} onChange={e => setForm(p => ({ ...p, rangoNitIni: e.target.value }))} inputProps={{ maxLength: 2 }} sx={{ flex: 1 }} />
            <TextField label="Rango NIT fin" value={form.rangoNitFin} onChange={e => setForm(p => ({ ...p, rangoNitFin: e.target.value }))} inputProps={{ maxLength: 2 }} sx={{ flex: 1 }} />
          </Box>
          <TextField label="Fecha límite" type="date" required value={form.fechaLimite} onChange={e => setForm(p => ({ ...p, fechaLimite: e.target.value }))} InputLabelProps={{ shrink: true }} />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogAbierto(false)} disabled={guardando}>Cancelar</Button>
          <Button variant="contained" disabled={!form.fechaLimite || guardando} loading={guardando} onClick={crear}>
            Crear
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        abierto={confirmacion !== null}
        titulo="Eliminar fecha del calendario"
        mensaje="¿Confirma que desea eliminar esta fecha del calendario? Esta acción no se puede deshacer."
        cargando={confirmacion !== null && eliminando === confirmacion}
        onConfirmar={confirmarEliminar}
        onCancelar={() => setConfirmacion(null)}
      />
    </>
  )
}
