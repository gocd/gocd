const webpack = require('webpack')
const CompressionPlugin = require('compression-webpack-plugin')
const Environment = require('../environment')

module.exports = class extends Environment {
  constructor() {
    super()

    this.plugins.set('ModuleConcatenation', new webpack.optimize.ModuleConcatenationPlugin())

    this.plugins.set('UglifyJs', new webpack.optimize.UglifyJsPlugin({
      sourceMap: true,
      compress: {
        warnings: false
      },
      output: {
        comments: false
      }
    }))

    this.plugins.set('Compression', new CompressionPlugin({
      asset: '[path].gz[query]',
      algorithm: 'gzip',
      test: /\.(js|css|html|json|ico|svg|eot|otf|ttf)$/
    }))
  }

  toWebpackConfig() {
    const result = super.toWebpackConfig()
    result.devtool = 'nosources-source-map'
    result.stats = 'normal'
    return result
  }
}
