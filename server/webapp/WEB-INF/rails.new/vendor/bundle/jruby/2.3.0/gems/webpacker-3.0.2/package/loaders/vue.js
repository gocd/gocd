const { dev_server: devServer } = require('../config')

const isProduction = process.env.NODE_ENV === 'production'
const extractCSS = !(devServer && devServer.hmr)

module.exports = {
  test: /\.vue(\.erb)?$/,
  loader: 'vue-loader',
  options: {
    extractCSS: isProduction || extractCSS
  }
}
