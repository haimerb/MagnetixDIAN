import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#0c4f82', light: '#1a7cc7', dark: '#083a5c' },
    secondary: { main: '#1a7cc7', light: '#4da3e0', dark: '#0e5a9e' },
    gold: { main: '#ffd200', light: '#ffe04d', dark: '#ccaa00' },
    terracotta: { main: '#b55a3a', light: '#d47a5a', dark: '#8c4428' },
    success: { main: '#2e7d32' },
    error: { main: '#c62828' },
    warning: { main: '#ed6c02' },
    text: { primary: '#1a1a1a', secondary: '#555555', disabled: '#999999' },
    background: { default: '#faf9f6', paper: '#ffffff' },
    divider: 'rgba(26,26,26,0.08)',
  },
  typography: {
    fontFamily: '"Work Sans", "Helvetica Neue", Arial, sans-serif',
    h1: { fontFamily: '"Fraunces", serif', fontWeight: 700 },
    h2: { fontFamily: '"Fraunces", serif', fontWeight: 600 },
    h3: { fontFamily: '"Fraunces", serif', fontWeight: 600 },
    h4: { fontFamily: '"Fraunces", serif', fontWeight: 600 },
    h5: { fontFamily: '"Fraunces", serif', fontWeight: 600 },
    h6: { fontFamily: '"Fraunces", serif', fontWeight: 600 },
    body1: { fontFamily: '"Work Sans", sans-serif' },
    body2: { fontFamily: '"Work Sans", sans-serif' },
    caption: { fontFamily: '"IBM Plex Mono", monospace' },
    overline: { fontFamily: '"IBM Plex Mono", monospace' },
  },
  shape: { borderRadius: 12 },
  spacing: 4,
  components: {
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { textTransform: 'none', minHeight: 44 },
      },
    },
    MuiCheckbox: { defaultProps: { color: 'primary' } },
    MuiTextField: { defaultProps: { variant: 'outlined', fullWidth: true } },
  },
})

export default theme