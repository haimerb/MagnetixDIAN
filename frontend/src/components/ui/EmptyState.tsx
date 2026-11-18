import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

type EmptyStateProps = {
  icon?: ReactNode
  title: string
  description?: string
  action?: ReactNode
}

export default function EmptyState({ icon, title, description, action }: EmptyStateProps) {
  return (
    <Box
      sx={{
        display: 'grid',
        justifyItems: 'center',
        textAlign: 'center',
        gap: 1.5,
        py: { xs: 4, md: 6 },
        px: 2,
      }}
    >
      {icon && (
        <Box sx={{ color: 'text.disabled', display: 'flex', '& svg': { fontSize: 44 } }}>{icon}</Box>
      )}
      <Typography variant="h6" component="p">
        {title}
      </Typography>
      {description && (
        <Typography variant="body2" color="text.secondary" sx={{ maxWidth: '48ch' }}>
          {description}
        </Typography>
      )}
      {action && <Box sx={{ mt: 1 }}>{action}</Box>}
    </Box>
  )
}
