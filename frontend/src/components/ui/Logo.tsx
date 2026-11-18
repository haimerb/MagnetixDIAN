import Box from '@mui/material/Box'
import Typography from '@mui/material/Typography'
import { useTheme } from '@mui/material/styles'

type LogoProps = {
  size?: number
  showWordmark?: boolean
  variant?: 'default' | 'onDark'
}

export default function Logo({ size = 34, showWordmark = true, variant = 'default' }: LogoProps) {
  const theme = useTheme()
  const onDark = variant === 'onDark'

  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 1.25 }}>
      <Box
        component="svg"
        viewBox="0 0 40 40"
        role="img"
        aria-label="MagnetixDIAN"
        sx={{ width: size, height: size, display: 'block', flexShrink: 0 }}
      >
        <rect
          x="2"
          y="2"
          width="36"
          height="36"
          rx="11"
          fill={onDark ? 'rgba(255,255,255,0.14)' : theme.palette.primary.main}
        />
        <path
          d="M11 28V13l9 8 9-8v15"
          fill="none"
          stroke="#ffffff"
          strokeWidth="3.4"
          strokeLinejoin="round"
          strokeLinecap="round"
        />
        <circle cx="30.5" cy="10.5" r="3.4" fill={theme.palette.secondary.main} />
      </Box>
      {showWordmark && (
        <Typography
          component="span"
          sx={{
            fontFamily: '"Space Grotesk", "Inter", sans-serif',
            fontWeight: 700,
            fontSize: size * 0.5,
            letterSpacing: '-0.02em',
            lineHeight: 1,
            color: onDark ? '#ffffff' : 'text.primary',
          }}
        >
          Magnetix
          <Box component="span" sx={{ color: onDark ? 'secondary.light' : 'secondary.main' }}>
            DIAN
          </Box>
        </Typography>
      )}
    </Box>
  )
}
