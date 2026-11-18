import Box from '@mui/material/Box'
import Card from '@mui/material/Card'
import CardContent from '@mui/material/CardContent'
import Typography from '@mui/material/Typography'
import type { ReactNode } from 'react'

type Tone = 'primary' | 'success' | 'warning' | 'error' | 'info'

type StatCardProps = {
  label: string
  value: ReactNode
  icon?: ReactNode
  tone?: Tone
  hint?: string
}

export default function StatCard({ label, value, icon, tone = 'primary', hint }: StatCardProps) {
  return (
    <Card sx={{ height: '100%', position: 'relative', overflow: 'hidden' }}>
      <Box sx={{ position: 'absolute', top: 0, bottom: 0, left: 0, width: 4, bgcolor: `${tone}.main` }} />
      <CardContent
        sx={{
          display: 'flex',
          alignItems: 'flex-start',
          justifyContent: 'space-between',
          gap: 2,
          pl: 2.5,
        }}
      >
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="caption" color="text.secondary" sx={{ display: 'block' }}>
            {label}
          </Typography>
          <Typography
            variant="h3"
            sx={{ mt: 1, fontFamily: '"Space Grotesk", "Inter", sans-serif' }}
          >
            {value}
          </Typography>
          {hint && (
            <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
              {hint}
            </Typography>
          )}
        </Box>
        {icon && (
          <Box sx={{ color: `${tone}.main`, display: 'flex', '& svg': { fontSize: 28 } }}>
            {icon}
          </Box>
        )}
      </CardContent>
    </Card>
  )
}
