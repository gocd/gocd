/*
 * Copyright 2017 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/* global __dirname */

'use strict';

var fs          = require('fs');
var fsExtra     = require('fs-extra');
var _           = require('lodash');
var process     = require('process');
var path        = require('path');
var webpack     = require('webpack');
var StatsPlugin = require('stats-webpack-plugin');

var singlePageAppModuleDir = path.join(__dirname, '..', 'webpack', 'single_page_apps');

var mithrilVersionFromNpm          = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'node_modules/mithril/package.json'), 'utf8')).version;
let mithrilPatchFileContentAsLines = fs.readFileSync(path.join(__dirname, '..', 'spec/webpack/patches/mithril.js'), 'utf8').split("\n");

let isMithrilVersionLine = function (line) {
  return _.includes(line, 'm.version = ');
};

var mithrilVersionFromPatch = _.find(mithrilPatchFileContentAsLines, isMithrilVersionLine)
  .split("=")[1].trim().replace(/"/g, "");

if (mithrilVersionFromNpm !== mithrilVersionFromPatch) {
  throw new Error("Please ensure that the patched mithril version is the same as the one installed via nodejs");
}

module.exports = function (env) {
  var production = (env && env.production === 'true');
  var outputDir  = env.outputDir || path.join(__dirname, '..', 'public', 'assets', 'webpack');

  console.log(`Generating assets in ${outputDir}`);

  var entries = _.reduce(fs.readdirSync(singlePageAppModuleDir), function (memo, file) {
    var fileName     = path.basename(file);
    var moduleName   = "single_page_apps/" + _.split(fileName, '.')[0];
    memo[moduleName] = path.join(singlePageAppModuleDir, file);
    return memo;
  }, {});

  var assetsDir = path.join(__dirname, '..', 'webpack');

  var plugins = [];
  plugins.push(new StatsPlugin('manifest.json', {
    chunkModules: false,
    source:       false,
    chunks:       false,
    modules:      false,
    assets:       true
  }));
  plugins.push(new webpack.ProvidePlugin({
    $:               "jquery",
    jQuery:          "jquery",
    "window.jQuery": "jquery"
  }));
  plugins.push(new webpack.optimize.CommonsChunkPlugin({
    name:      "vendor-and-helpers.chunk",
    filename:  production ? '[name]-[chunkhash].js' : '[name].js',
    minChunks: function (module, _count) {
      function isFromNPM() {
        return new RegExp(`node_modules`).test(module.resource);
      }

      function isInside() {
        return fs.realpathSync(module.resource).indexOf(fs.realpathSync(path.join(assetsDir, ..._(Array.prototype.concat.apply([], arguments)).flattenDeep().compact().value()))) === 0;
      }

      return module.resource && (isFromNPM() || isInside('helpers') || isInside('gen') || isInside('models', 'shared') || isInside('models', 'mixins') || isInside('views', 'shared'));
    },
  }));

  if (production) {
    plugins.push(new webpack.optimize.UglifyJsPlugin({
      compressor: {
        warnings: false
      },
      sourceMap:  false
    }));
    plugins.push(new webpack.LoaderOptionsPlugin({minimize: true}));
    plugins.push(new webpack.DefinePlugin({'process.env': {NODE_ENV: JSON.stringify('production')}}));
    plugins.push(new webpack.NoEmitOnErrorsPlugin());
  }


  var config = {
    cache:     true,
    bail:      true,
    entry:     entries,
    output:    {
      path:       outputDir,
      publicPath: '/go/assets/webpack/',
      filename:   production ? '[name]-[chunkhash].js' : '[name].js'
    },
    resolve:   {
      modules:    _.compact([
        assetsDir,
        production ? null : path.join(__dirname, '..', 'spec', 'webpack', 'patches'), // provide monkey patches libs for tests
        'node_modules']),
      extensions: ['.js', '.js.msx', '.msx', '.es6'],
      alias:      {
        'string-plus': 'helpers/string-plus',
        'string':      'underscore.string',
        'jQuery':      'jquery',
      }
    },
    devServer: {
      hot:    false,
      inline: false
    },
    plugins:   plugins,
    module:    {
      rules: [
        {
          test:    /\.msx$/,
          exclude: /node_modules/,
          use:     [
            {
              loader:  'babel-loader',
              options: {
                cacheDirectory: true
              },
            }
          ]
        },
        {
          test:    /\.js$/,
          exclude: /node_modules/,
          use:     [
            {
              loader:  'babel-loader',
              options: {
                cacheDirectory: true
              },
            }
          ]
        }
      ]
    }
  };

  if (production) {
    fsExtra.removeSync(config.output.path);
  } else {
    config.devtool = "inline-source-map";

    config.resolve.modules.push(path.join(__dirname, 'spec', 'webpack'));

    var jasmineCore  = require('jasmine-core');
    var jasmineFiles = jasmineCore.files;

    var HtmlWebpackPlugin = require('html-webpack-plugin');

    var jasmineIndexPage = {
      inject:          true,
      xhtml:           true,
      filename:        '_specRunner.html',
      template:        path.join(__dirname, '..', 'spec', 'webpack', '_specRunner.html.ejs'),
      jasmineJsFiles:  _.map(jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles), function (file) {
        return '__jasmine/' + file;
      }),
      jasmineCssFiles: _.map(jasmineFiles.cssFiles, function (file) {
        return '__jasmine/' + file;
      }),
      excludeChunks:   _.keys(entries)
    };

    config.plugins.push(new HtmlWebpackPlugin(jasmineIndexPage));

    config.entry['specRoot'] = path.join(__dirname, '..', 'spec', 'webpack', 'specRoot.js');

    var JasmineAssetsPlugin = function (options) {
      this.apply = function (compiler) {
        compiler.plugin('emit', function (compilation, callback) {
          var allJasmineAssets = jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles).concat(jasmineFiles.cssFiles);

          _.each(allJasmineAssets, function (asset) {
            var file = path.join(jasmineFiles.path, asset);

            var contents = fs.readFileSync(file).toString();

            compilation.assets['__jasmine/' + asset] = {
              source: function () {
                return contents;
              },
              size:   function () {
                return contents.length;
              }
            };
          });

          callback();
        });
      };
    };

    config.plugins.push(new JasmineAssetsPlugin());

  }

  return config;
};
