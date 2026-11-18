import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import Divider from '@mui/material/Divider'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

type SectionCardProps = {
  title?: string
  subtitle?: string
  actions?: ReactNode
  children: ReactNode
  padded?: boolean
}

export default function SectionCard({
  title,
  subtitle,
  actions,
  children,
  padded = true,
}: SectionCardProps) {
  const hasHeader = Boolean(title || actions)

  return (
    <Card>
      {hasHeader && (
        <>
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              gap: 2,
              px: { xs: 2, md: 3 },
              py: 2,
            }}
          >
            <Box sx={{ minWidth: 0 }}>
              {title && (
                <Typography variant="h6" component="h2">
                  {title}
                </Typography>
              )}
              {subtitle && (
                <Typography variant="body2" color="text.secondary">
                  {subtitle}
                </Typography>
              )}
            </Box>
            {actions && (
              <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>{actions}</Box>
            )}
          </Box>
          <Divider />
        </>
      )}
      <Box sx={{ p: padded ? { xs: 2, md: 3 } : 0 }}>{children}</Box>
    </Card>
  )
}
