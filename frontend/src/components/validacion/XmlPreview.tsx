import Box from '@mui/material/Box'
import SectionCard from '../ui/SectionCard'

export default function XmlPreview({ xml }: { xml: string }) {
  return (
    <SectionCard title="XML generado" padded={false}>
      <Box
        component="pre"
        sx={{
          m: 0,
          p: { xs: 2, md: 3 },
          whiteSpace: 'pre-wrap',
          wordBreak: 'break-word',
          fontFamily: '"JetBrains Mono", monospace',
          fontSize: '0.8125rem',
          bgcolor: (t) => t.palette.action.hover,
          borderRadius: '0 0 10px 10px',
        }}
      >
        {xml}
      </Box>
    </SectionCard>
  )
}
