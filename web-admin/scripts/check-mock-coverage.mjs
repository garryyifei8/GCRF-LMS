#!/usr/bin/env node
/* eslint-env node */
/**
 * MSW Mock Coverage Check Script
 * Compares API endpoints defined in src/api/*.js with MSW handlers in src/mock/handlers/*.js
 * Exits with code 1 if there are missing handlers or method mismatches.
 */

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const rootDir = path.resolve(__dirname, '..')

// ANSI color codes
const RED = '\x1b[31m'
const YELLOW = '\x1b[33m'
const GREEN = '\x1b[32m'
const CYAN = '\x1b[36m'
const RESET = '\x1b[0m'
const BOLD = '\x1b[1m'

// ─────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────

function normalizeUrl(url) {
  // Replace template literal expressions ${...} with :param
  let normalized = url.replace(/\$\{[^}]+\}/g, ':param')
  // Replace MSW path params :foo with :param for comparison
  // (we keep them as-is for display, only normalize for set membership)
  // Trim trailing slashes
  normalized = normalized.replace(/\/+$/, '')
  // Ensure starts with /
  if (!normalized.startsWith('/')) normalized = '/' + normalized
  return normalized
}

function normalizeUrlForKey(url) {
  // Normalize both template literal params AND MSW :param style to `:param`
  let normalized = url.replace(/\$\{[^}]+\}/g, ':param')
  normalized = normalized.replace(/:[\w]+/g, ':param')
  normalized = normalized.replace(/\/+$/, '')
  if (!normalized.startsWith('/')) normalized = '/' + normalized
  return normalized
}

function readFile(filePath) {
  return fs.readFileSync(filePath, 'utf-8')
}

function getJsFiles(dir) {
  if (!fs.existsSync(dir)) return []
  return fs
    .readdirSync(dir)
    .filter((f) => f.endsWith('.js'))
    .map((f) => path.join(dir, f))
}

// ─────────────────────────────────────────────
// Extract API endpoints from src/api/*.js
// ─────────────────────────────────────────────

function extractApiEndpoints(apiDir) {
  const files = getJsFiles(apiDir)
  const endpoints = []

  for (const filePath of files) {
    const content = readFile(filePath)
    const fileName = path.relative(path.join(rootDir, 'src'), filePath)

    // Match request({...}) blocks - handle multiline
    // Strategy: find all request({ ... }) calls and extract url + method from each
    const requestCallRegex = /request\(\s*\{([^}]+(?:\{[^}]*\}[^}]*)*)\}\s*\)/g
    let match

    while ((match = requestCallRegex.exec(content)) !== null) {
      const block = match[1]

      // Extract url from the block (handles string literals and template literals)
      const urlMatch = block.match(/url\s*:\s*(?:'([^']*)'|"([^"]*)"|`([^`]*)`)/)
      // Extract method from the block
      const methodMatch = block.match(/method\s*:\s*['"]([^'"]+)['"]/)

      if (urlMatch && methodMatch) {
        const rawUrl = urlMatch[1] || urlMatch[2] || urlMatch[3]
        const method = methodMatch[1].toUpperCase()
        const normalizedUrl = normalizeUrl(rawUrl)
        const keyUrl = normalizeUrlForKey(rawUrl)

        endpoints.push({
          method,
          rawUrl,
          normalizedUrl,
          keyUrl,
          key: `${method} ${keyUrl}`,
          source: fileName
        })
      }
    }
  }

  return endpoints
}

// ─────────────────────────────────────────────
// Extract MSW handlers from src/mock/handlers/*.js
// ─────────────────────────────────────────────

function extractMswHandlers(handlersDir) {
  const files = getJsFiles(handlersDir)
  const handlers = []

  for (const filePath of files) {
    const content = readFile(filePath)
    const fileName = path.relative(path.join(rootDir, 'src'), filePath)

    // Match http.METHOD('url', ...) or http.METHOD(`url`, ...)
    const handlerRegex = /http\.(get|post|put|delete|patch)\(\s*(?:'([^']*)'|"([^"]*)"|`([^`]*)`)/gi
    let match

    while ((match = handlerRegex.exec(content)) !== null) {
      const method = match[1].toUpperCase()
      const rawUrl = match[2] || match[3] || match[4]
      const normalizedUrl = normalizeUrl(rawUrl)
      const keyUrl = normalizeUrlForKey(rawUrl)

      handlers.push({
        method,
        rawUrl,
        normalizedUrl,
        keyUrl,
        key: `${method} ${keyUrl}`,
        source: fileName
      })
    }
  }

  return handlers
}

// ─────────────────────────────────────────────
// Deduplicate (same key = same entry)
// ─────────────────────────────────────────────

function deduplicateByKey(items) {
  const seen = new Map()
  for (const item of items) {
    if (!seen.has(item.key)) {
      seen.set(item.key, item)
    }
  }
  return Array.from(seen.values())
}

// ─────────────────────────────────────────────
// Main
// ─────────────────────────────────────────────

function main() {
  const apiDir = path.join(rootDir, 'src', 'api')
  const handlersDir = path.join(rootDir, 'src', 'mock', 'handlers')

  console.log(`\n${BOLD}${CYAN}=== MSW Mock Coverage Report ===${RESET}\n`)

  // Extract
  const rawApiEndpoints = extractApiEndpoints(apiDir)
  const rawHandlers = extractMswHandlers(handlersDir)

  // Deduplicate
  const apiEndpoints = deduplicateByKey(rawApiEndpoints)
  const mswHandlers = deduplicateByKey(rawHandlers)

  console.log(`Frontend API endpoints: ${BOLD}${apiEndpoints.length}${RESET}`)
  console.log(`MSW handlers:           ${BOLD}${mswHandlers.length}${RESET}`)
  console.log()

  // Build lookup maps
  const apiByKey = new Map(apiEndpoints.map((e) => [e.key, e]))
  const handlerByKey = new Map(mswHandlers.map((h) => [h.key, h]))

  // Also build URL-only maps for method mismatch detection
  const apiByUrl = new Map()
  for (const ep of apiEndpoints) {
    if (!apiByUrl.has(ep.keyUrl)) apiByUrl.set(ep.keyUrl, [])
    apiByUrl.get(ep.keyUrl).push(ep)
  }

  const handlerByUrl = new Map()
  for (const h of mswHandlers) {
    if (!handlerByUrl.has(h.keyUrl)) handlerByUrl.set(h.keyUrl, [])
    handlerByUrl.get(h.keyUrl).push(h)
  }

  // Missing: API endpoints with no matching handler (same method + normalized URL)
  const missing = apiEndpoints.filter((ep) => !handlerByKey.has(ep.key))

  // Orphan: MSW handlers with no matching API endpoint
  const orphan = mswHandlers.filter((h) => !apiByKey.has(h.key))

  // Method mismatch: same normalized URL exists in both, but different methods
  // (already excluded from missing/orphan if method matches — here we find cross-method overlaps)
  const methodMismatches = []
  for (const ep of apiEndpoints) {
    if (!handlerByKey.has(ep.key)) {
      // Check if the URL exists in handlers but with a different method
      const handlersAtUrl = handlerByUrl.get(ep.keyUrl) || []
      for (const h of handlersAtUrl) {
        if (h.method !== ep.method) {
          methodMismatches.push({
            url: ep.normalizedUrl,
            apiMethod: ep.method,
            handlerMethod: h.method,
            apiSource: ep.source,
            handlerSource: h.source
          })
        }
      }
    }
  }

  // Deduplicate method mismatches by URL+apiMethod+handlerMethod
  const seenMismatches = new Set()
  const uniqueMismatches = methodMismatches.filter((m) => {
    const key = `${m.apiMethod}|${m.handlerMethod}|${m.url}`
    if (seenMismatches.has(key)) return false
    seenMismatches.add(key)
    return true
  })

  // ── Missing handlers ──
  if (missing.length > 0) {
    console.log(`${RED}❌ Missing handlers (${missing.length}):${RESET}`)
    for (const ep of missing) {
      const method = ep.method.padEnd(7)
      console.log(
        `  ${RED}${method}${RESET} ${ep.normalizedUrl.padEnd(60)} ${YELLOW}(${ep.source})${RESET}`
      )
    }
    console.log()
  } else {
    console.log(`${GREEN}✓ No missing handlers${RESET}\n`)
  }

  // ── Orphan handlers ──
  if (orphan.length > 0) {
    console.log(`${YELLOW}⚠ Orphan handlers (${orphan.length}):${RESET}`)
    for (const h of orphan) {
      const method = h.method.padEnd(7)
      console.log(
        `  ${YELLOW}${method}${RESET} ${h.normalizedUrl.padEnd(60)} ${YELLOW}(${h.source})${RESET}`
      )
    }
    console.log()
  } else {
    console.log(`${GREEN}✓ No orphan handlers${RESET}\n`)
  }

  // ── Method mismatches ──
  if (uniqueMismatches.length > 0) {
    console.log(`${RED}❌ Method mismatches (${uniqueMismatches.length}):${RESET}`)
    for (const m of uniqueMismatches) {
      console.log(
        `  ${m.url.padEnd(60)} API:${RED}${m.apiMethod}${RESET} vs Handler:${YELLOW}${m.handlerMethod}${RESET}`
      )
      console.log(`    API source:     ${m.apiSource}`)
      console.log(`    Handler source: ${m.handlerSource}`)
    }
    console.log()
  } else {
    console.log(`${GREEN}✓ Method matches: all good${RESET}\n`)
  }

  // ── Coverage summary ──
  const covered = apiEndpoints.length - missing.length
  const pct = apiEndpoints.length > 0 ? ((covered / apiEndpoints.length) * 100).toFixed(1) : '100.0'
  const coverageColor = parseFloat(pct) >= 80 ? GREEN : parseFloat(pct) >= 60 ? YELLOW : RED
  console.log(
    `${BOLD}Coverage: ${covered}/${apiEndpoints.length} = ${coverageColor}${pct}%${RESET}\n`
  )

  // Exit code
  if (missing.length > 0 || uniqueMismatches.length > 0) {
    process.exit(1)
  }
  process.exit(0)
}

main()
