import Chip from '@mui/material/Chip'

const ESTADOS: Record<string, { color: 'default' | 'primary' | 'success' | 'warning' | 'error' | 'info'; label: string }> = {
  BORRADOR: { color: 'default', label: 'Borrador' },
  CARGADO: { color: 'info', label: 'Cargado' },
  VALIDADO: { color: 'success', label: 'Validado' },
  VALIDADO_CON_ERRORES: { color: 'warning', label: 'Con errores' },
  XML_GENERADO: { color: 'primary', label: 'XML generado' },
  PRESENTADO: { color: 'success', label: 'Presentado' },
}

export default function EstadoChip({ estado }: { estado: string }) {
  const cfg = ESTADOS[estado] ?? { color: 'default' as const, label: estado }
  return <Chip size="small" color={cfg.color} label={cfg.label} />
}