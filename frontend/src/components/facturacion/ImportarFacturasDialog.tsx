import { useRef, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogTitle from '@mui/material/DialogTitle'
import List from '@mui/material/List'
import ListItem from '@mui/material/ListItem'
import ListItemText from '@mui/material/ListItemText'
import Stack from '@mui/material/Stack'
import Typography from '@mui/material/Typography'
import DownloadOutlinedIcon from '@mui/icons-material/DownloadOutlined'
import UploadFileOutlinedIcon from '@mui/icons-material/UploadFileOutlined'
import { importarFacturas, type ResultadoImportacion } from '../../api/client'

const PLANTILLA = [
  'grupo;tipo_documento;adquirente_tipo_documento;adquirente_numero_documento;adquirente_digito_verificacion;adquirente_nombre;adquirente_email;adquirente_direccion;adquirente_municipio;adquirente_regimen;adquirente_responsabilidad_fiscal;codigo_producto;descripcion;cantidad;unidad_medida;valor_unitario;descuento;iva_pct;inc_pct;unspsc;forma_pago;medio_pago;fecha_vencimiento;observaciones',
  'F001;01;31;900123456;0;Cliente Ejemplo S.A.S;cliente@ejemplo.com;Calle 1 # 2-3;Bogotá;ORDINARIO;R-99-PN;SERV-001;Servicio de consultoría;2;94;1500000;0;19;0;80101500;1;10;2026-12-31;Factura de prueba',
  'F001;01;31;900123456;0;Cliente Ejemplo S.A.S;cliente@ejemplo.com;Calle 1 # 2-3;Bogotá;ORDINARIO;R-99-PN;SERV-002;Soporte mensual;1;94;500000;0;19;0;80101500;1;10;2026-12-31;Factura de prueba',
].join('\r\n')

type Props = {
  abierto: boolean
  empresaId: number
  onCerrar: () => void
  onImportado: () => void
}

export default function ImportarFacturasDialog({ abierto, empresaId, onCerrar, onImportado }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [archivo, setArchivo] = useState<File | null>(null)
  const [resultado, setResultado] = useState<ResultadoImportacion | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [importando, setImportando] = useState(false)

  const cerrar = () => {
    if (importando) return
    setArchivo(null)
    setResultado(null)
    setError(null)
    onCerrar()
  }

  const descargarPlantilla = () => {
    const blob = new Blob(['\uFEFF' + PLANTILLA], { type: 'text/csv;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'plantilla-facturas.csv'
    a.click()
    URL.revokeObjectURL(url)
  }

  const importar = async () => {
    if (!archivo) return
    setImportando(true)
    setError(null)
    try {
      const data = await importarFacturas(empresaId, archivo)
      setResultado(data)
      if (data.facturasEmitidas > 0) {
        onImportado()
      }
    } catch (e: unknown) {
      const detalle = (e as { response?: { data?: { message?: string } } }).response?.data?.message
      setError(detalle ?? 'No se pudo importar el archivo.')
    } finally {
      setImportando(false)
    }
  }

  return (
    <Dialog open={abierto} onClose={cerrar} maxWidth="sm" fullWidth>
      <DialogTitle>Importar facturas</DialogTitle>
      <DialogContent sx={{ display: 'grid', gap: 2, pt: '16px !important' }}>
        <Alert severity="info">
          Cargue un archivo CSV o Excel con una fila por línea de factura. Las filas con el mismo
          <strong> grupo</strong> se agrupan en un solo documento. Columnas obligatorias: grupo,
          adquirente_numero_documento, adquirente_nombre, descripcion, cantidad y valor_unitario.
        </Alert>

        <Button variant="outlined" startIcon={<DownloadOutlinedIcon />} onClick={descargarPlantilla}>
          Descargar plantilla
        </Button>

        <Stack direction="row" spacing={1} sx={{ alignItems: 'center' }}>
          <Button variant="outlined" component="label" startIcon={<UploadFileOutlinedIcon />}>
            Seleccionar archivo
            <input
              ref={inputRef}
              type="file"
              hidden
              accept=".csv,.xlsx"
              onChange={(e) => {
                setArchivo(e.target.files?.[0] ?? null)
                setResultado(null)
              }}
            />
          </Button>
          <Typography variant="body2" color="text.secondary">
            {archivo ? archivo.name : 'Ningún archivo seleccionado'}
          </Typography>
        </Stack>

        {error && <Alert severity="error">{error}</Alert>}

        {resultado && (
          <Box>
            <Alert severity={resultado.facturasEmitidas > 0 ? 'success' : 'warning'}>
              {resultado.facturasEmitidas} factura(s) emitida(s), {resultado.facturasRechazadas} rechazada(s),{' '}
              {resultado.lineasImportadas} línea(s) importada(s).
            </Alert>
            {resultado.errores.length > 0 && (
              <List dense sx={{ maxHeight: 180, overflow: 'auto', mt: 1 }}>
                {resultado.errores.map((e, i) => (
                  <ListItem key={`${i}-${e}`} disableGutters>
                    <ListItemText primary={e} slotProps={{ primary: { variant: 'body2' } }} />
                  </ListItem>
                ))}
              </List>
            )}
          </Box>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={cerrar} disabled={importando}>Cerrar</Button>
        <Button variant="contained" onClick={importar} disabled={!archivo || importando} loading={importando}>
          Importar
        </Button>
      </DialogActions>
    </Dialog>
  )
}
