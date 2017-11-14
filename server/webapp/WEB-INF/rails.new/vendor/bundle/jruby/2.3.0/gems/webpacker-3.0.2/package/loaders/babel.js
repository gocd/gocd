const { join } = require('path')
const { cache_path } = require('../config')

module.exports = {
  test: /\.(js|jsx)?(\.erb)?$/,
  exclude: /node_modules/,
  loader: 'babel-loader',
  options: {
    cacheDirectory: join(cache_path, 'babel-loader')
  }
}
