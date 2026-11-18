import Box from '@mui/material/Box'
import Button from '@mui/material/Button'
import Typography from '@mui/material/Typography'
import { Component, type ErrorInfo, type ReactNode } from 'react'

type Props = { children: ReactNode }
type State = { error: Error | null }

export default class ErrorBoundary extends Component<Props, State> {
  state: State = { error: null }

  static getDerivedStateFromError(error: Error): State {
    return { error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    if (import.meta.env.DEV) {
      console.error('Error no controlado:', error, info.componentStack)
    }
  }

  render() {
    if (this.state.error) {
      return (
        <Box
          sx={{
            display: 'grid',
            placeItems: 'center',
            gap: 2,
            minHeight: '100vh',
            p: 4,
            textAlign: 'center',
          }}
        >
          <Typography variant="h2">Algo salió mal</Typography>
          <Typography color="text.secondary">
            Se produjo un error inesperado. Intente recargar la aplicación.
          </Typography>
          <Button variant="contained" onClick={() => window.location.assign('/')}>
            Volver al inicio
          </Button>
        </Box>
      )
    }
    return this.props.children
  }
}
