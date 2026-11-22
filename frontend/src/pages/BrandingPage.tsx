import { useEffect, useState } from 'react'
import Alert from '@mui/material/Alert'
import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Grid from '@mui/material/Grid2'
import Stack from '@mui/material/Stack'
import TextField from '@mui/material/TextField'
import Typography from '@mui/material/Typography'
import CloudSyncOutlinedIcon from '@mui/icons-material/CloudSyncOutlined'
import SaveOutlinedIcon from '@mui/icons-material/SaveOutlined'
import { guardarBranding, obtenerBranding, obtenerBrandingPublico } from '../api/client'
import type { Branding, BrandingRequest } from '../api/client'
import LoadingState from '../components/ui/LoadingState'
import Logo from '../components/ui/Logo'
import PageHeader from '../components/ui/PageHeader'
import SectionCard from '../components/ui/SectionCard'
import { useEmpresaActiva } from '../features/auth/useEmpresaActiva'
import { useNotificaciones } from '../features/notificaciones/contexto'
import { useThemeMode } from '../theme/useThemeMode'

const COLORES: { clave: keyof Pick<BrandingRequest, 'primaryColor' | 'accentColor' | 'pwaThemeColor' | 'pwaBackgroundColor'>; etiqueta: string }[] = [
  { clave: 'primaryColor', etiqueta: 'Color principal' },
  { clave: 'accentColor', etiqueta: 'Color de acento' },
  { clave: 'pwaThemeColor', etiqueta: 'Color de la PWA' },
  { clave: 'pwaBackgroundColor', etiqueta: 'Fondo de la PWA' },
]

export default function BrandingPage({ ocultarEncabezado = false }: { ocultarEncabezado?: boolean }) {
  const { empresaId, cargando: cargandoEmpresa } = useEmpresaActiva()
  const { notificar } = useNotificaciones()
  const { actualizarBranding } = useThemeMode()
  const [form, setForm] = useState<BrandingRequest | null>(null)
  const [publico, setPublico] = useState<Branding | null>(null)
  const [guardando, setGuardando] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    if (empresaId == null) return
    let activo = true
    obtenerBranding(empresaId)
      .then((datos) => {
        if (!activo) return
        setForm({
          publicSlug: datos.publicSlug,
          displayName: datos.displayName,
          logoUrl: datos.logoUrl,
          primaryColor: datos.primaryColor,
          accentColor: datos.accentColor,
          fontFamily: datos.fontFamily,
          invoiceFooter: datos.invoiceFooter,
          whatsappNumber: datos.whatsappNumber,
          whatsappTemplate: datos.whatsappTemplate,
          pwaThemeColor: datos.pwaThemeColor,
          pwaBackgroundColor: datos.pwaBackgroundColor,
        })
      })
      .catch(() => {
        if (activo) setError('No se pudo cargar la configuración de marca.')
      })
    return () => {
      activo = false
    }
  }, [empresaId])

  const actualizar = <K extends keyof BrandingRequest>(clave: K, valor: BrandingRequest[K]) => {
    setForm((actual) => (actual ? { ...actual, [clave]: valor } : actual))
  }

  const onGuardar = async () => {
    if (empresaId == null || !form) return
    setGuardando(true)
    setError(null)
    try {
      const respuesta = await guardarBranding(empresaId, form)
      setForm({
        publicSlug: respuesta.publicSlug,
        displayName: respuesta.displayName,
        logoUrl: respuesta.logoUrl,
        primaryColor: respuesta.primaryColor,
        accentColor: respuesta.accentColor,
        fontFamily: respuesta.fontFamily,
        invoiceFooter: respuesta.invoiceFooter,
        whatsappNumber: respuesta.whatsappNumber,
        whatsappTemplate: respuesta.whatsappTemplate,
        pwaThemeColor: respuesta.pwaThemeColor,
        pwaBackgroundColor: respuesta.pwaBackgroundColor,
      })
      actualizarBranding(respuesta)
      notificar('Marca actualizada.', 'success')
    } finally {
      setGuardando(false)
    }
  }

  const onVerPublico = async () => {
    if (!form?.publicSlug) return
    setError(null)
    try {
      setPublico(await obtenerBrandingPublico(form.publicSlug))
    } catch {
      setError('No se encontró una configuración pública para ese identificador.')
    }
  }

  return (
    <Box component="section" sx={{ display: 'grid', gap: 3 }}>
      {!ocultarEncabezado && (
        <PageHeader
          overline="Personalización"
          title="Identidad visual"
          subtitle="Personalice logo, colores, fuente, pie de factura y plantilla de WhatsApp. Los cambios se aplican a todo el tema."
          actions={
            <Button
              variant="outlined"
              startIcon={<CloudSyncOutlinedIcon />}
              disabled={!form?.publicSlug}
              onClick={onVerPublico}
            >
              Ver versión pública
            </Button>
          }
        />
      )}

      {empresaId == null && !cargandoEmpresa && (
        <Alert severity="info">No hay una empresa asociada para configurar la marca.</Alert>
      )}
      {error && <Alert severity="error">{error}</Alert>}

      {form == null ? (
        <LoadingState label="Cargando configuración de marca…" compacto />
      ) : (
        <>
          <SectionCard title="Vista previa" subtitle="Así se verá la marca en la aplicación">
            <Box
              sx={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                p: 4,
                borderRadius: 2,
                bgcolor: form.pwaBackgroundColor,
              }}
            >
              <Logo
                size={56}
                branding={{
                  displayName: form.displayName,
                  logoUrl: form.logoUrl,
                  primaryColor: form.primaryColor,
                  accentColor: form.accentColor,
                }}
              />
            </Box>
          </SectionCard>

          <SectionCard
            title="Identidad visual"
            subtitle="Identificador público, nombre, logo y tipografía"
            actions={
              <Button
                variant="contained"
                startIcon={<SaveOutlinedIcon />}
                loading={guardando}
                onClick={onGuardar}
              >
                Guardar
              </Button>
            }
          >
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Identificador público"
                  value={form.publicSlug}
                  onChange={(event) => actualizar('publicSlug', event.target.value)}
                  helperText="Se usa en la URL pública de la marca."
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Nombre visible"
                  value={form.displayName ?? ''}
                  onChange={(event) => actualizar('displayName', event.target.value || null)}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="URL del logo"
                  value={form.logoUrl ?? ''}
                  onChange={(event) => actualizar('logoUrl', event.target.value || null)}
                  helperText="Se conserva automáticamente si no se modifica."
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Fuente"
                  value={form.fontFamily}
                  onChange={(event) => actualizar('fontFamily', event.target.value)}
                />
              </Grid>
            </Grid>
          </SectionCard>

          <SectionCard title="Colores" subtitle="Se aplican al tema, facturas y PWA">
            <Grid container spacing={3}>
              {COLORES.map((color) => (
                <Grid key={color.clave} size={{ xs: 12, sm: 6, md: 3 }}>
                  <Stack spacing={1}>
                    <Typography variant="body2">{color.etiqueta}</Typography>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      <Box
                        component="input"
                        type="color"
                        aria-label={color.etiqueta}
                        value={form[color.clave]}
                        onChange={(event: React.ChangeEvent<HTMLInputElement>) =>
                          actualizar(color.clave, event.target.value)
                        }
                        sx={{
                          width: 56,
                          height: 40,
                          p: 0,
                          border: '1px solid',
                          borderColor: 'divider',
                          borderRadius: 1,
                          bgcolor: 'background.paper',
                        }}
                      />
                      <Typography variant="caption">{form[color.clave]}</Typography>
                    </Box>
                  </Stack>
                </Grid>
              ))}
            </Grid>
          </SectionCard>

          <SectionCard title="Mensajes" subtitle="Pie de factura y plantilla de WhatsApp">
            <Grid container spacing={2}>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Pie de factura"
                  value={form.invoiceFooter ?? ''}
                  onChange={(event) => actualizar('invoiceFooter', event.target.value || null)}
                  multiline
                  minRows={3}
                />
              </Grid>
              <Grid size={{ xs: 12, md: 6 }}>
                <TextField
                  label="Número de WhatsApp"
                  value={form.whatsappNumber ?? ''}
                  onChange={(event) => actualizar('whatsappNumber', event.target.value || null)}
                />
              </Grid>
              <Grid size={{ xs: 12 }}>
                <TextField
                  label="Plantilla de WhatsApp"
                  value={form.whatsappTemplate ?? ''}
                  onChange={(event) => actualizar('whatsappTemplate', event.target.value || null)}
                  multiline
                  minRows={3}
                />
              </Grid>
            </Grid>
          </SectionCard>
        </>
      )}

      {publico && (
        <SectionCard title="Configuración pública" subtitle={`Identificador: ${publico.publicSlug}`}>
          <Alert severity="success">
            {publico.displayName ?? 'MagnetixDIAN'} · {publico.primaryColor} · {publico.accentColor}
          </Alert>
        </SectionCard>
      )}
    </Box>
  )
}
