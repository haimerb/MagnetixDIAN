import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

type PageHeaderProps = {
  title: string
  subtitle?: ReactNode
  overline?: string
  actions?: ReactNode
}

export default function PageHeader({ title, subtitle, overline, actions }: PageHeaderProps) {
  return (
    <Box
      sx={{
        display: 'flex',
        flexWrap: 'wrap',
        gap: 2,
        alignItems: 'flex-end',
        justifyContent: 'space-between',
      }}
    >
      <Box sx={{ minWidth: 0 }}>
        {overline && (
          <Typography
            variant="overline"
            sx={{ display: 'block', color: 'secondary.dark', mb: 0.5 }}
          >
            {overline}
          </Typography>
        )}
        <Typography variant="h4" component="h1">
          {title}
        </Typography>
        {subtitle && (
          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mt: 0.75, maxWidth: '68ch' }}
          >
            {subtitle}
          </Typography>
        )}
      </Box>
      {actions && (
        <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1, alignItems: 'center' }}>
          {actions}
        </Box>
      )}
    </Box>
  )
}
