const { join } = require('path')
const { source_path } = require('../config')
const assetHost = require('../asset_host')

module.exports = {
  test: /\.(jpg|jpeg|png|gif|svg|eot|otf|ttf|woff|woff2)$/i,
  use: [{
    loader: 'file-loader',
    options: {
      name: '[path][name]-[hash].[ext]',
      context: join(source_path),
      publicPath: assetHost.publicPathWithHost
    }
  }]
}
