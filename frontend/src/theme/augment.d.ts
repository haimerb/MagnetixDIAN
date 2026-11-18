import '@mui/material/styles'

declare module '@mui/material/styles' {
  interface Palette {
    amber: Palette['primary']
    clay: Palette['primary']
  }
  interface PaletteOptions {
    amber?: PaletteOptions['primary']
    clay?: PaletteOptions['primary']
  }
}

declare module '@mui/material/Chip' {
  interface ChipPropsColorOverrides {
    amber: true
    clay: true
  }
}

declare module '@mui/material/Button' {
  interface ButtonPropsColorOverrides {
    amber: true
    clay: true
  }
}
