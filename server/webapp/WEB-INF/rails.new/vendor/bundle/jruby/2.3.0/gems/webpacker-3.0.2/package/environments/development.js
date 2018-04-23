const webpack = require('webpack')
const Environment = require('../environment')
const { dev_server: devServer } = require('../config')
const assetHost = require('../asset_host')

module.exports = class extends Environment {
  constructor() {
    super()

    if (devServer.hmr) {
      this.plugins.set('HotModuleReplacement', new webpack.HotModuleReplacementPlugin())
      this.plugins.set('NamedModules', new webpack.NamedModulesPlugin())
    }
  }

  toWebpackConfig() {
    const result = super.toWebpackConfig()
    if (devServer.hmr) {
      result.output.filename = '[name]-[hash].js'
    }
    result.output.pathinfo = true
    result.devtool = 'cheap-eval-source-map'
    result.devServer = {
      clientLogLevel: 'none',
      compress: true,
      disableHostCheck: devServer.disable_host_check,
      host: devServer.host,
      port: devServer.port,
      https: devServer.https,
      hot: devServer.hmr,
      contentBase: assetHost.path,
      inline: devServer.inline,
      useLocalIp: devServer.use_local_ip,
      public: devServer.public,
      publicPath: assetHost.publicPath,
      historyApiFallback: true,
      headers: {
        'Access-Control-Allow-Origin': '*'
      },
      overlay: devServer.overlay,
      watchOptions: {
        ignored: /node_modules/
      },
      stats: {
        errorDetails: true
      }
    }
    return result
  }
}
