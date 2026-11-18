import { describe, expect, it } from 'vitest'
import theme from './theme'

describe('theme', () => {
  it('define los colores de marca (petroleo y ambar)', () => {
    expect(theme.palette.primary.main).toBe('#0E5C55')
    expect(theme.palette.secondary.main).toBe('#D98A2B')
  })

  it('usa Space Grotesk en los titulos e Inter en el texto', () => {
    expect(theme.typography.h1.fontFamily).toContain('Space Grotesk')
    expect(theme.typography.body1.fontFamily).toContain('Inter')
  })
})
