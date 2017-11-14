/* eslint global-require: 0 */
/* eslint import/no-dynamic-require: 0 */

const { basename, dirname, join, relative, resolve } = require('path')
const { sync } = require('glob')
const extname = require('path-complete-extname')

const webpack = require('webpack')
const ExtractTextPlugin = require('extract-text-webpack-plugin')
const ManifestPlugin = require('webpack-manifest-plugin')

const config = require('./config')
const assetHost = require('./asset_host')

function getLoaderMap() {
  const result = new Map()
  const paths = sync(resolve(__dirname, 'loaders', '*.js'))
  paths.forEach((path) => {
    const name = basename(path, extname(path))
    result.set(name, require(path))
  })
  return result
}

function getPluginMap() {
  const result = new Map()
  result.set('Environment', new webpack.EnvironmentPlugin(JSON.parse(JSON.stringify(process.env))))
  result.set('ExtractText', new ExtractTextPlugin('[name]-[contenthash].css'))
  result.set('Manifest', new ManifestPlugin({ publicPath: assetHost.publicPath, writeToFileEmit: true }))
  return result
}

function getExtensionsGlob() {
  const { extensions } = config
  if (!extensions.length) {
    throw new Error('You must configure at least one extension to compile in webpacker.yml')
  }
  return extensions.length === 1 ? `**/${extensions[0]}` : `**/*{${extensions.join(',')}}`
}

function getEntryObject() {
  const result = {}
  const glob = getExtensionsGlob()
  const rootPath = join(config.source_path, config.source_entry_path)
  const paths = sync(join(rootPath, glob))
  paths.forEach((path) => {
    const namespace = relative(join(rootPath), dirname(path))
    const name = join(namespace, basename(path, extname(path)))
    result[name] = resolve(path)
  })
  return result
}

function getModulePaths() {
  let result = [resolve(config.source_path), 'node_modules']
  if (config.resolved_paths) {
    result = result.concat(config.resolved_paths)
  }
  return result
}

module.exports = class Environment {
  constructor() {
    this.loaders = getLoaderMap()
    this.plugins = getPluginMap()
  }

  toWebpackConfig() {
    return {
      entry: getEntryObject(),

      output: {
        filename: '[name]-[chunkhash].js',
        chunkFilename: '[name]-[chunkhash].chunk.js',
        path: assetHost.path,
        publicPath: assetHost.publicPath
      },

      module: {
        rules: Array.from(this.loaders.values())
      },

      plugins: Array.from(this.plugins.values()),

      resolve: {
        extensions: config.extensions,
        modules: getModulePaths()
      },

      resolveLoader: {
        modules: ['node_modules']
      }
    }
  }
}
