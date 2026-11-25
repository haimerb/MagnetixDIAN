export {
  aceptarInvitacion,
  listarInvitaciones,
  reenviarVerificacionEmail,
  verificarEmail,
} from '../client'
export type { Invitacion, ReenviarVerificacionResponse, VerificacionEmail } from '../client'

export function tokenDeVerificacion(valor: string | null): string | null {
  return valor?.trim() || null
}
