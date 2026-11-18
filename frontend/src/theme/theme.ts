import { createTheme } from '@mui/material/styles'

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#0E5C55', light: '#2E8B82', dark: '#093F3A', contrastText: '#ffffff' },
    secondary: { main: '#D98A2B', light: '#EBB05F', dark: '#A6651A', contrastText: '#1a1a1a' },
    amber: { main: '#D98A2B', light: '#EBB05F', dark: '#A6651A', contrastText: '#1a1a1a' },
    clay: { main: '#C05746', light: '#D67F70', dark: '#8F3E31', contrastText: '#ffffff' },
    success: { main: '#1F7A4D', light: '#4FA377', dark: '#155435' },
    error: { main: '#C0392B', light: '#D86A5E', dark: '#8F281D' },
    warning: { main: '#B7791F', light: '#D19B4A', dark: '#8A5A15' },
    info: { main: '#2563A8', light: '#5A8CCB', dark: '#1A477A' },
    text: { primary: '#0F1D1B', secondary: '#4C5D5A', disabled: '#93A29F' },
    background: { default: '#F1F5F4', paper: '#ffffff' },
    divider: 'rgba(15,29,27,0.10)',
  },
  typography: {
    fontFamily: '"Inter", system-ui, -apple-system, "Segoe UI", Arial, sans-serif',
    h1: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 700, letterSpacing: '-0.02em' },
    h2: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 700, letterSpacing: '-0.02em' },
    h3: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 600, letterSpacing: '-0.01em' },
    h4: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 600, letterSpacing: '-0.01em' },
    h5: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 600 },
    h6: { fontFamily: '"Space Grotesk", "Inter", sans-serif', fontWeight: 600 },
    subtitle1: { fontWeight: 600 },
    subtitle2: { fontWeight: 600 },
    body1: { fontFamily: '"Inter", sans-serif' },
    body2: { fontFamily: '"Inter", sans-serif' },
    button: { fontWeight: 600 },
    caption: { fontFamily: '"JetBrains Mono", monospace', letterSpacing: 0 },
    overline: { fontFamily: '"JetBrains Mono", monospace', letterSpacing: '0.08em' },
  },
  shape: { borderRadius: 10 },
  spacing: 4,
  components: {
    MuiCssBaseline: {
      styleOverrides: {
        ':root': { colorScheme: 'light' },
        '::selection': { backgroundColor: 'rgba(14,92,85,0.16)' },
        '@media (prefers-reduced-motion: reduce)': {
          '*, *::before, *::after': {
            animationDuration: '0.01ms !important',
            transitionDuration: '0.01ms !important',
          },
        },
      },
    },
    MuiButton: {
      defaultProps: { disableElevation: true },
      styleOverrides: {
        root: { textTransform: 'none', minHeight: 42, borderRadius: 10, paddingInline: 18 },
        sizeSmall: { minHeight: 34 },
      },
    },
    MuiCheckbox: { defaultProps: { color: 'primary' } },
    MuiTextField: { defaultProps: { variant: 'outlined', fullWidth: true } },
    MuiPaper: {
      styleOverrides: {
        root: { backgroundImage: 'none' },
        outlined: { borderColor: 'rgba(15,29,27,0.10)' },
      },
    },
    MuiCard: {
      defaultProps: { elevation: 0 },
      styleOverrides: {
        root: {
          border: '1px solid rgba(15,29,27,0.10)',
          boxShadow: '0 1px 2px rgba(15,29,27,0.04)',
          transition: 'border-color 200ms ease, box-shadow 200ms ease',
        },
      },
    },
    MuiChip: { styleOverrides: { root: { fontWeight: 500 } } },
    MuiTableCell: {
      styleOverrides: {
        head: { fontWeight: 600, color: '#4C5D5A' },
      },
    },
    MuiTooltip: {
      styleOverrides: {
        tooltip: { backgroundColor: '#0F1D1B', fontSize: '0.75rem' },
      },
    },
  },
})

export default theme
