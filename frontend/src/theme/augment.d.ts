import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface Palette {
    gold: Palette['primary']
    terracotta: Palette['primary']
  }
  interface PaletteOptions {
    gold?: PaletteOptions['primary']
    terracotta?: PaletteOptions['primary']
  }
}

declare module '@mui/material/Chip' {
  interface ChipPropsColorOverrides {
    gold: true
    terracotta: true
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    gold: true
    terracotta: true
  }
}