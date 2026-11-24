import React from 'react'
import ReactDOM from 'react-dom/client'
import * as Sentry from '@sentry/react'
import '@fontsource/inter/latin-400.css'
import '@fontsource/inter/latin-500.css'
import '@fontsource/inter/latin-600.css'
import '@fontsource/space-grotesk/latin-500.css'
import '@fontsource/space-grotesk/latin-600.css'
import '@fontsource/space-grotesk/latin-700.css'
import '@fontsource/jetbrains-mono/latin-400.css'
import '@fontsource/jetbrains-mono/latin-500.css'
import App from './App'
import ErrorBoundary from './components/ErrorBoundary'
import { NotificacionesProvider } from './features/notificaciones/NotificacionesProvider'
import ThemeModeProvider from './theme/ThemeModeProvider'

const sentryDsn = import.meta.env.VITE_SENTRY_DSN as string | undefined
if (sentryDsn) {
  Sentry.init({
    dsn: sentryDsn,
    environment: import.meta.env.MODE,
    tracesSampleRate: 0.1,
    dataCollection: { userInfo: false },
  })
}

if (import.meta.env.PROD && 'serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    const scriptBundle = document.querySelector<HTMLScriptElement>('script[type="module"][src*="/assets/index-"]')
    const versionBundle = scriptBundle?.src.match(/index-([a-zA-Z0-9_-]+)\.js/)?.[1]
    const rutaSw = versionBundle ? `/sw-${versionBundle}.js` : '/sw.js'
    navigator.serviceWorker.register(rutaSw).catch(() => undefined)
  })
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <ThemeModeProvider>
      <ErrorBoundary>
        <NotificacionesProvider>
          <App />
        </NotificacionesProvider>
      </ErrorBoundary>
    </ThemeModeProvider>
  </React.StrictMode>,
)