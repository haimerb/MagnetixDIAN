import { describe, expect, it } from 'vitest'
import theme, { createAppTheme } from './theme'

describe('theme', () => {
  it('define los colores de marca (petroleo y ambar)', () => {
    expect(theme.palette.primary.main).toBe('#0E5C55')
    expect(theme.palette.secondary.main).toBe('#D98A2B')
  })

  it('usa Space Grotesk en los titulos e Inter en el texto', () => {
    expect(theme.typography.h1.fontFamily).toContain('Space Grotesk')
    expect(theme.typography.body1.fontFamily).toContain('Inter')
  })

  it('el tema claro usa fondo claro y el oscuro fondo oscuro', () => {
    expect(createAppTheme('light').palette.mode).toBe('light')
    const oscuro = createAppTheme('dark')
    expect(oscuro.palette.mode).toBe('dark')
    expect(oscuro.palette.background.default).toBe('#0C1614')
    expect(oscuro.palette.background.default).not.toBe(theme.palette.background.default)
  })
})
