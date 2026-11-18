import Box from '@mui/material/Box'
import Step from '@mui/material/Step'
import StepContent from '@mui/material/StepContent'
import StepLabel from '@mui/material/StepLabel'
import Stepper from '@mui/material/Stepper'
import Typography from '@mui/material/Typography'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'

const PASOS = [
  {
    titulo: 'Valide sus datos en MagnetixDIAN',
    texto:
      'Cargue el Excel, ejecute la validación y corrija todos los errores. Solo los registros sin errores entran al XML final.',
  },
  {
    titulo: 'Genere y descargue el XML',
    texto:
      'Desde la sección Validación, genere el XML y descárguelo. El archivo resultante respeta el esquema del formato 1001.',
  },
  {
    titulo: 'Ingrese a la página de la DIAN',
    texto:
      'Ingrese al portal MUISCA (www.muisca.dian.gov.co) con su usuario y contraseña habilitados para presentación de información exógena.',
  },
  {
    titulo: 'Ubique el servicio de información exógena',
    texto:
      'En el menú principal, diríjase a «Información exógena» → «Presentar información exógena» (o el equivalente del año gravable vigente).',
  },
  {
    titulo: 'Adjunte el archivo XML generado',
    texto:
      'Seleccione el formato 1001 y el año gravable correspondiente, adjunte el XML descargado y confirme los datos del informante.',
  },
  {
    titulo: 'Revise el acuse de recibo',
    texto:
      'La DIAN emitirá un recibo de presentación con número de radicado. Consérvelo como soporte. Verifique que el reporte quedó «Enviado».',
  },
]

export default function GuiaMuiscaPage() {
  return (
    <Box component="section" sx={{ maxWidth: 780, display: 'grid', gap: 3 }}>
      <PageHeader
        overline="Ayuda"
        title="Guía paso a paso — Presentación en MUISCA"
        subtitle="Cómo presentar su información exógena generada con MagnetixDIAN ante la DIAN. Los plazos dependen del tipo de contribuyente y del último dígito del NIT según el calendario tributario vigente."
      />

      <SectionCard>
        <Stepper orientation="vertical" activeStep={-1} nonLinear>
          {PASOS.map((paso) => (
            <Step key={paso.titulo} expanded>
              <StepLabel>
                <Typography variant="subtitle2">{paso.titulo}</Typography>
              </StepLabel>
              <StepContent>
                <Typography variant="body2" color="text.secondary">
                  {paso.texto}
                </Typography>
              </StepContent>
            </Step>
          ))}
        </Stepper>
      </SectionCard>

      <SectionCard title="Plazos orientativos — año gravable 2025 (presentación 2026)">
        <Box component="ul" sx={{ m: 0, pl: 2.5, display: 'grid', gap: 1 }}>
          <Typography component="li" variant="body2" color="text.secondary">
            Grandes contribuyentes: 28 de abril al 13 de mayo de 2026 (según último dígito NIT).
          </Typography>
          <Typography component="li" variant="body2" color="text.secondary">
            Personas jurídicas y naturales: 14 de mayo al 12 de junio de 2026 (según dos últimos dígitos NIT).
          </Typography>
          <Typography component="li" variant="body2" color="text.secondary">
            Consulte siempre la resolución anual vigente de la DIAN.
          </Typography>
        </Box>
      </SectionCard>
    </Box>
  )
}
