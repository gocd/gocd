const { resolve } = require('path')
const { safeLoad } = require('js-yaml')
const { readFileSync } = require('fs')

const filePath = resolve('config', 'webpacker.yml')
const config = safeLoad(readFileSync(filePath), 'utf8')[process.env.NODE_ENV]

const isBoolean = str => /^true/.test(str) || /^false/.test(str)

const fetch = key =>
  (isBoolean(process.env[key]) ? JSON.parse(process.env[key]) : process.env[key])

const devServer = (key) => {
  const envValue = fetch(`WEBPACKER_DEV_SERVER_${key.toUpperCase().replace(/_/g, '')}`)
  if (typeof envValue === 'undefined' || envValue === null) return config.dev_server[key]
  return envValue
}

if (config.dev_server) {
  Object.keys(config.dev_server).forEach((key) => {
    config.dev_server[key] = devServer(key)
  })
}

module.exports = config
