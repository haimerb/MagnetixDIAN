import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import FormControl from '@mui/material/FormControl'
import InputLabel from '@mui/material/InputLabel'
import MenuItem from '@mui/material/MenuItem'
import Select from '@mui/material/Select'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import ToggleButton from '@mui/material/ToggleButton'
import ToggleButtonGroup from '@mui/material/ToggleButtonGroup'
import Typography from '@mui/material/Typography'
import type { MetodoPago } from '../../api/client'

export type CamposMetodoPago = {
  celular: string
  financialInstitutionId: string
  paymentType: string
  documentType: string
  documentNumber: string
  payerName: string
}

const OPCIONES: { valor: MetodoPago; etiqueta: string }[] = [
  { valor: 'CARD', etiqueta: 'Tarjeta' },
  { valor: 'NEQUI', etiqueta: 'Nequi' },
  { valor: 'PSE', etiqueta: 'PSE' },
  { valor: 'BREB_QR', etiqueta: 'Bre-B QR' },
]

const BANCOS = [
  { valor: 'BANCOLOMBIA', etiqueta: 'Bancolombia' },
  { valor: 'DAVIVIENDA', etiqueta: 'Davivienda' },
  { valor: 'BBVA', etiqueta: 'BBVA' },
  { valor: 'BANCO_DE_BOGOTA', etiqueta: 'Banco de Bogotá' },
  { valor: 'OTRO', etiqueta: 'Otra entidad' },
]

const TIPOS_DOCUMENTO = [
  { valor: 'CC', etiqueta: 'Cédula de ciudadanía' },
  { valor: 'CE', etiqueta: 'Cédula de extranjería' },
  { valor: 'NIT', etiqueta: 'NIT' },
  { valor: 'PA', etiqueta: 'Pasaporte' },
]

type MedioPagoSelectorProps = {
  metodo: MetodoPago
  onMetodoChange: (metodo: MetodoPago) => void
  campos: CamposMetodoPago
  onCamposChange: (campos: CamposMetodoPago) => void
  titulo?: string
  descripcion?: string
}

export default function MedioPagoSelector({
  metodo,
  onMetodoChange,
  campos,
  onCamposChange,
  titulo = 'Método de pago',
  descripcion,
}: MedioPagoSelectorProps) {
  const actualizar = (campo: keyof CamposMetodoPago, valor: string) => {
    onCamposChange({ ...campos, [campo]: valor })
  }

  return (
    <Stack spacing={2}>
      <Box>
        <Typography variant="subtitle1" component="h3" sx={{ mb: 0.5 }}>
          {titulo}
        </Typography>
        {descripcion && (
          <Typography variant="body2" color="text.secondary">
            {descripcion}
          </Typography>
        )}
      </Box>

      <ToggleButtonGroup
        exclusive
        fullWidth
        size="small"
        value={metodo}
        aria-label={titulo}
        onChange={(_, valor) => {
          if (valor) onMetodoChange(valor as MetodoPago)
        }}
        sx={{ flexWrap: 'wrap', '& .MuiToggleButton-root': { flex: '1 1 140px' } }}
      >
        {OPCIONES.map((opcion) => (
          <ToggleButton key={opcion.valor} value={opcion.valor} aria-label={opcion.etiqueta}>
            {opcion.etiqueta}
          </ToggleButton>
        ))}
      </ToggleButtonGroup>

      {metodo === 'CARD' && (
        <Alert severity="info">
          Los datos de la tarjeta se diligencian en la pasarela segura; no se almacenan aquí.
        </Alert>
      )}

      {metodo === 'NEQUI' && (
        <Stack spacing={2}>
          <TextField
            id="nequi-celular"
            label="Celular Nequi"
            value={campos.celular}
            onChange={(e) => actualizar('celular', e.target.value)}
            inputProps={{ inputMode: 'tel', autoComplete: 'tel' }}
            fullWidth
          />
          <TextField
            id="nequi-pagador"
            label="Nombre del pagador"
            value={campos.payerName}
            onChange={(e) => actualizar('payerName', e.target.value)}
            autoComplete="name"
            fullWidth
          />
        </Stack>
      )}

      {metodo === 'PSE' && (
        <Stack spacing={2}>
          <FormControl fullWidth>
            <InputLabel id="pse-banco-label">Entidad financiera</InputLabel>
            <Select
              labelId="pse-banco-label"
              id="pse-banco"
              label="Entidad financiera"
              value={campos.financialInstitutionId}
              onChange={(e) => actualizar('financialInstitutionId', e.target.value)}
            >
              {BANCOS.map((banco) => (
                <MenuItem key={banco.valor} value={banco.valor}>
                  {banco.etiqueta}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel id="pse-tipo-persona-label">Tipo de persona</InputLabel>
            <Select
              labelId="pse-tipo-persona-label"
              id="pse-tipo-persona"
              label="Tipo de persona"
              value={campos.paymentType}
              onChange={(e) => actualizar('paymentType', e.target.value)}
            >
              <MenuItem value="NATURAL">Natural</MenuItem>
              <MenuItem value="JURIDICA">Jurídica</MenuItem>
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel id="pse-tipo-documento-label">Tipo de documento</InputLabel>
            <Select
              labelId="pse-tipo-documento-label"
              id="pse-tipo-documento"
              label="Tipo de documento"
              value={campos.documentType}
              onChange={(e) => actualizar('documentType', e.target.value)}
            >
              {TIPOS_DOCUMENTO.map((tipo) => (
                <MenuItem key={tipo.valor} value={tipo.valor}>
                  {tipo.etiqueta}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <TextField
            id="pse-documento"
            label="Número de documento"
            value={campos.documentNumber}
            onChange={(e) => actualizar('documentNumber', e.target.value)}
            fullWidth
          />
        </Stack>
      )}

      {metodo === 'BREB_QR' && (
        <Stack spacing={2}>
          <TextField
            id="breb-celular"
            label="Celular o llave Bre-B"
            value={campos.celular}
            onChange={(e) => actualizar('celular', e.target.value)}
            inputProps={{ inputMode: 'tel', autoComplete: 'tel' }}
            fullWidth
          />
          <TextField
            id="breb-pagador"
            label="Nombre del pagador"
            value={campos.payerName}
            onChange={(e) => actualizar('payerName', e.target.value)}
            autoComplete="name"
            fullWidth
          />
        </Stack>
      )}
    </Stack>
  )
}
