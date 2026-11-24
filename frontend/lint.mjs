import { spawnSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'

const args = process.argv.slice(2).filter((arg) => arg !== '--')
const eslint = fileURLToPath(new URL('./node_modules/eslint/bin/eslint.js', import.meta.url))
const result = spawnSync(
  process.execPath,
  [eslint, '.', '--ext', 'ts,tsx', '--report-unused-disable-directives', '--max-warnings=0', ...args],
  { stdio: 'inherit' },
)
process.exit(result.status ?? 1)
