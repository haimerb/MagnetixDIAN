import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link as RouterLink } from 'react-router-dom'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import FormControlLabel from '@mui/material/FormControlLabel'
import Checkbox from '@mui/material/Checkbox'
import Grid from '@mui/material/Grid2'
import MenuItem from '@mui/material/MenuItem'
import Stack from '@mui/material/Stack'
import Table from '@mui/material/Table'
import TableBody from '@mui/material/TableBody'
import TableCell from '@mui/material/TableCell'
import TableContainer from '@mui/material/TableContainer'
import TableHead from '@mui/material/TableHead'
import TableRow from '@mui/material/TableRow'
import TextField from '@mui/material/TextField'
import AccountTreeOutlinedIcon from '@mui/icons-material/AccountTreeOutlined'
import DescriptionOutlinedIcon from '@mui/icons-material/DescriptionOutlined'
import FactCheckOutlinedIcon from '@mui/icons-material/FactCheckOutlined'
import ReceiptLongOutlinedIcon from '@mui/icons-material/ReceiptLongOutlined'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import {
  causarDocumentoRadian,
  contabilizarDocumentoRadian,
  listarDocumentosRadian,
  traerDocumentosDianPorArchivo,
  traerDocumentosDianPorToken,
} from '../api/client'
import type { DocumentoRadian } from '../api/client'
import ConfirmDialog from '../components/ui/ConfirmDialog'
import EmptyState from '../components/ui/EmptyState'
import EstadoChip from '../components/ui/EstadoChip'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import StatCard from '../components/ui/StatCard'
import TableSkeleton from '../components/ui/TableSkeleton'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import { money } from '../features/facturacion/calculos'

type Accion = { tipo: 'causar' | 'contabilizar'; documento: DocumentoRadian }

const DIRECCIONES = ['EMITIDO', 'RECIBIDO']
type ModoTraida = 'TOKEN' | 'ARCHIVO'

function fechaLocal(fecha: Date) {
  const anio = fecha.getFullYear()
  const mes = String(fecha.getMonth() + 1).padStart(2, '0')
  const dia = String(fecha.getDate()).padStart(2, '0')
  return `${anio}-${mes}-${dia}`
}

function keyDocumento(documento: DocumentoRadian) {
  return String(documento.id ?? documento.cufeCude ?? documento.consecutivo)
}

export default function RadianPage() {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const [documentos, setDocumentos] = useState<DocumentoRadian[]>([])
  const [cargando, setCargando] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [filtro, setFiltro] = useState('TODOS')
  const [recarga, setRecarga] = useState(0)
  const [traidaAbierta, setTraidaAbierta] = useState(false)
  const [modoTraida, setModoTraida] = useState<ModoTraida>('TOKEN')
  const [archivo, setArchivo] = useState<File | null>(null)
  const [token, setToken] = useState('')
  const [fechaDesde, setFechaDesde] = useState(() => fechaLocal(new Date(new Date().getFullYear(), new Date().getMonth(), 1)))
  const [fechaHasta, setFechaHasta] = useState(() => fechaLocal(new Date()))
  const [grupo, setGrupo] = useState('')
  const [nitTercero, setNitTercero] = useState('')
  const [guardarToken, setGuardarToken] = useState(true)
  const [direccion, setDireccion] = useState('RECIBIDO')
  const [trayendo, setTrayendo] = useState(false)
  const [accion, setAccion] = useState<Accion | null>(null)
  const [procesando, setProcesando] = useState(false)

  const cargar = useCallback(() => {
    if (empresaId == null) {
      setCargando(false)
      return
    }
    setCargando(true)
    setError(null)
    listarDocumentosRadian(empresaId)
      .then(setDocumentos)
      .catch(() => setError('No se pudieron cargar los documentos de la DIAN.'))
      .finally(() => setCargando(false))
  }, [empresaId])

  useEffect(() => {
    cargar()
  }, [cargar, recarga])

  const filtrados = useMemo(
    () => (filtro === 'TODOS' ? documentos : documentos.filter((d) => d.direccion === filtro)),
    [documentos, filtro],
  )

  const resumen = useMemo(
    () => ({
      total: documentos.length,
      pendientes: documentos.filter((d) => d.estadoCausacion === 'PENDIENTE').length,
      contabilizados: documentos.filter((d) => d.asientoContableId != null).length,
    }),
    [documentos],
  )

  const volverACargar = () => setRecarga((n) => n + 1)

  const onTraer = async () => {
    if (empresaId == null) return
    if (modoTraida === 'ARCHIVO' && !archivo) return
    if (modoTraida === 'TOKEN' && !token.trim()) return
    setTrayendo(true)
    try {
      const resultado = modoTraida === 'TOKEN'
        ? await traerDocumentosDianPorToken(empresaId, {
            token: token.trim(),
            direccion,
            fechaDesde,
            fechaHasta,
            grupo: grupo.trim() || undefined,
            nitTercero: nitTercero.trim() || undefined,
            guardarToken,
          })
        : await traerDocumentosDianPorArchivo(
            empresaId,
            archivo as File,
            fechaDesde,
            fechaHasta,
            direccion,
            grupo.trim() || undefined,
            nitTercero.trim() || undefined,
          )
      notificar(`${resultado.documentos} documento(s) traídos de la DIAN.`, 'success')
      setTraidaAbierta(false)
      if (modoTraida === 'ARCHIVO') setArchivo(null)
      volverACargar()
    } catch {
      notificar('No se pudieron traer los documentos de la DIAN.', 'error')
    } finally {
      setTrayendo(false)
    }
  }

  const onConfirmarAccion = async () => {
    if (empresaId == null || !accion) return
    setProcesando(true)
    try {
      if (accion.tipo === 'causar') {
        await causarDocumentoRadian(empresaId, accion.documento.id)
        notificar(`Documento ${accion.documento.consecutivo} causado.`, 'success')
      } else {
        await contabilizarDocumentoRadian(empresaId, accion.documento.id)
        notificar(`Asiento generado para ${accion.documento.consecutivo}.`, 'success')
      }
      setAccion(null)
      volverACargar()
    } finally {
      setProcesando(false)
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Contabilidad"
        title="Asentar la facturación"
        subtitle="Traiga facturas, notas y documentos soporte de la DIAN, cáuselos y asiente sus asientos contables."
        actions={
          <>
            <Button
              component={RouterLink}
              to="/app/contabilidad"
              variant="outlined"
              startIcon={<ReceiptLongOutlinedIcon />}
            >
              Asientos contables
            </Button>
            <Button
              variant="contained"
              startIcon={<UploadFileOutlinedIcon />}
              disabled={empresaId == null}
              onClick={() => setTraidaAbierta(true)}
            >
              Traer de la DIAN
            </Button>
          </>
        }
      />

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard label="Documentos" value={resumen.total} icon={<DescriptionOutlinedIcon />} />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Pendientes de causación"
            value={resumen.pendientes}
            tone="warning"
            icon={<FactCheckOutlinedIcon />}
          />
        </Grid>
        <Grid size={{ xs: 12, sm: 6, md: 4 }}>
          <StatCard
            label="Contabilizados"
            value={resumen.contabilizados}
            tone="success"
            icon={<AccountTreeOutlinedIcon />}
          />
        </Grid>
      </Grid>

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para consultar documentos de la DIAN.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      <SectionCard
        title="Bandeja de documentos"
        subtitle="Causación y asentamiento por documento"
        actions={
          <TextField
            select
            size="small"
            label="Dirección"
            value={filtro}
            onChange={(event) => setFiltro(event.target.value)}
            sx={{ minWidth: 160 }}
          >
            <MenuItem value="TODOS">Todas</MenuItem>
            {DIRECCIONES.map((valor) => (
              <MenuItem key={valor} value={valor}>
                {valor === 'EMITIDO' ? 'Emitidos' : 'Recibidos'}
              </MenuItem>
            ))}
          </TextField>
        }
        padded={false}
      >
        {cargando || cargandoEmpresa ? (
          <TableSkeleton filas={5} columnas={9} label="Cargando documentos de la DIAN…" />
        ) : filtrados.length === 0 ? (
          <EmptyState
            icon={<DescriptionOutlinedIcon />}
            title="Sin documentos de la DIAN"
            description="Traiga los listados desde la DIAN con su token o con un archivo exportado."
          />
        ) : (
          <TableContainer>
            <Table size="small" aria-label="Documentos de la DIAN" sx={{ minWidth: 980 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Consecutivo</TableCell>
                  <TableCell>Contraparte</TableCell>
                  <TableCell>Tipo</TableCell>
                  <TableCell>Fecha</TableCell>
                  <TableCell align="right">Total</TableCell>
                  <TableCell>DIAN</TableCell>
                  <TableCell>Causación</TableCell>
                  <TableCell>Contabilización</TableCell>
                  <TableCell align="right">Acciones</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {filtrados.map((documento) => (
                  <TableRow key={keyDocumento(documento)} hover>
                    <TableCell sx={{ fontWeight: 600 }}>{documento.consecutivo}</TableCell>
                    <TableCell>
                      <Stack spacing={0}>
                        <Box component="span" sx={{ fontSize: '0.875rem' }}>
                          {documento.razonSocialContraparte || documento.nitContraparte}
                        </Box>
                        <Box component="span" sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                          {documento.nitContraparte} · {documento.direccion}
                        </Box>
                      </Stack>
                    </TableCell>
                    <TableCell>{documento.tipoDocumento}</TableCell>
                    <TableCell>{documento.fecha}</TableCell>
                    <TableCell align="right">{money(documento.total)}</TableCell>
                    <TableCell>
                      <EstadoChip estado={documento.estadoRadian} />
                    </TableCell>
                    <TableCell>
                      <EstadoChip estado={documento.estadoCausacion} />
                    </TableCell>
                    <TableCell>
                      <Stack spacing={0}>
                        <EstadoChip estado={documento.estadoContabilizacion || 'PENDIENTE'} />
                        {documento.asientoContableId != null && (
                          <Box component="span" sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                            Asiento #{documento.asientoContableId}
                          </Box>
                        )}
                      </Stack>
                    </TableCell>
                    <TableCell align="right">
                      <Stack direction="row" spacing={1} justifyContent="flex-end">
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={documento.estadoCausacion !== 'PENDIENTE'}
                          onClick={() => setAccion({ tipo: 'causar', documento })}
                        >
                          Causar
                        </Button>
                        <Button
                          size="small"
                          variant="outlined"
                          disabled={documento.asientoContableId != null}
                          onClick={() => setAccion({ tipo: 'contabilizar', documento })}
                        >
                          Asentar
                        </Button>
                      </Stack>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </SectionCard>

      <Dialog
        open={traidaAbierta}
        onClose={() => {
          if (!trayendo) setTraidaAbierta(false)
        }}
        fullWidth
        maxWidth="sm"
      >
        <DialogTitle>Traer de la DIAN</DialogTitle>
        <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
          <Stack direction="row" spacing={1}>
            <Button
              variant={modoTraida === 'TOKEN' ? 'contained' : 'outlined'}
              onClick={() => setModoTraida('TOKEN')}
            >
              Token o enlace
            </Button>
            <Button
              variant={modoTraida === 'ARCHIVO' ? 'contained' : 'outlined'}
              onClick={() => setModoTraida('ARCHIVO')}
            >
              Archivo Excel
            </Button>
          </Stack>

          {modoTraida === 'TOKEN' ? (
            <>
              <TextField
                label="Token o enlace de la DIAN"
                type="password"
                value={token}
                onChange={(event) => setToken(event.target.value)}
                helperText="Se guarda cifrado y nunca se muestra en pantalla."
                fullWidth
              />
              <FormControlLabel
                control={
                  <Checkbox
                    checked={guardarToken}
                    onChange={(event) => setGuardarToken(event.target.checked)}
                  />
                }
                label="Guardar para próximas consultas"
              />
            </>
          ) : (
            <>
              <Button variant="outlined" component="label" startIcon={<UploadFileOutlinedIcon />}>
                Seleccionar archivo
                <input
                  type="file"
                  hidden
                  accept=".csv,.xlsx,.xls,.json,.zip"
                  onChange={(event) => setArchivo(event.target.files?.[0] ?? null)}
                />
              </Button>
              {archivo && <Alert severity="info">Archivo seleccionado: {archivo.name}</Alert>}
            </>
          )}

          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                select
                label="Dirección"
                value={direccion}
                onChange={(event) => setDireccion(event.target.value)}
                fullWidth
              >
                {DIRECCIONES.map((valor) => (
                  <MenuItem key={valor} value={valor}>
                    {valor === 'EMITIDO' ? 'Emitidos' : 'Recibidos'}
                  </MenuItem>
                ))}
              </TextField>
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Grupo (opcional)"
                value={grupo}
                onChange={(event) => setGrupo(event.target.value)}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Desde"
                type="date"
                value={fechaDesde}
                onChange={(event) => setFechaDesde(event.target.value)}
                InputLabelProps={{ shrink: true }}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                label="Hasta"
                type="date"
                value={fechaHasta}
                onChange={(event) => setFechaHasta(event.target.value)}
                InputLabelProps={{ shrink: true }}
                fullWidth
              />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <TextField
                label="NIT tercero (opcional)"
                value={nitTercero}
                onChange={(event) => setNitTercero(event.target.value)}
                fullWidth
              />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setTraidaAbierta(false)} disabled={trayendo}>
            Cancelar
          </Button>
          <Button
            variant="contained"
            onClick={onTraer}
            disabled={
              !fechaDesde ||
              !fechaHasta ||
              (modoTraida === 'TOKEN' ? !token.trim() : !archivo)
            }
            loading={trayendo}
          >
            Traer documentos
          </Button>
        </DialogActions>
      </Dialog>

      <ConfirmDialog
        abierto={accion != null}
        titulo={accion?.tipo === 'causar' ? 'Causar documento' : 'Asentar documento'}
        mensaje={
          accion
            ? `Se ${accion.tipo === 'causar' ? 'causará' : 'asentará'} el documento ${accion.documento.consecutivo}. ¿Desea continuar?`
            : ''
        }
        textoConfirmar="Confirmar"
        color="primary"
        cargando={procesando}
        onConfirmar={onConfirmarAccion}
        onCancelar={() => setAccion(null)}
      />
    </Box>
  )
}
