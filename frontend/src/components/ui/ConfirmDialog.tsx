import Button from '@mui/material/Button'
import Dialog from '@mui/material/Dialog'
import DialogActions from '@mui/material/DialogActions'
import DialogContent from '@mui/material/DialogContent'
import DialogContentText from '@mui/material/DialogContentText'
import DialogTitle from '@mui/material/DialogTitle'

type ConfirmDialogProps = {
  abierto: boolean
  titulo: string
  mensaje: string
  textoConfirmar?: string
  textoCancelar?: string
  color?: 'error' | 'primary' | 'warning'
  cargando?: boolean
  onConfirmar: () => void
  onCancelar: () => void
}

export default function ConfirmDialog({
  abierto,
  titulo,
  mensaje,
  textoConfirmar = 'Eliminar',
  textoCancelar = 'Cancelar',
  color = 'error',
  cargando = false,
  onConfirmar,
  onCancelar,
}: ConfirmDialogProps) {
  return (
    <Dialog open={abierto} onClose={cargando ? undefined : onCancelar} maxWidth="xs" fullWidth>
      <DialogTitle>{titulo}</DialogTitle>
      <DialogContent>
        <DialogContentText>{mensaje}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onCancelar} disabled={cargando}>
          {textoCancelar}
        </Button>
        <Button variant="contained" color={color} onClick={onConfirmar} loading={cargando}>
          {textoConfirmar}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
