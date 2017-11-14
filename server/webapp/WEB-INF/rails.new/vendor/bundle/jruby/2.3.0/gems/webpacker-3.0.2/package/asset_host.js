const config = require('./config')
const { resolve } = require('path')

function removeOuterSlashes(string) {
  return string.replace(/^\/*/, '').replace(/\/*$/, '')
}

function formatPublicPath(host = '', path = '') {
  let formattedHost = removeOuterSlashes(host)
  if (formattedHost && !/^http/i.test(formattedHost)) {
    formattedHost = `//${formattedHost}`
  }
  const formattedPath = removeOuterSlashes(path)
  return `${formattedHost}/${formattedPath}/`
}

module.exports = {
  path: resolve('public', config.public_output_path),
  publicPath: `/${config.public_output_path}/`.replace(/([^:]\/)\/+/g, '$1'),
  publicPathWithHost: formatPublicPath(process.env.ASSET_HOST, config.public_output_path)
}
