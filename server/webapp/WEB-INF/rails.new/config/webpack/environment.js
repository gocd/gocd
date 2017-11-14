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

const {environment} = require('@rails/webpacker');
const StatsPlugin   = require('stats-webpack-plugin');
const webpack       = require('webpack');
const fs            = require('fs');
const fsExtra       = require('fs-extra');
const _             = require('lodash');
const path          = require('path');
const webpackerYml  = require('@rails/webpacker/package/config');
const assetHost     = require('@rails/webpacker/package/asset_host');
const assetsDir     = path.join(__dirname, '..', '..', webpackerYml.source_path);

const mithrilVersionFromNpm          = JSON.parse(fs.readFileSync(path.join(__dirname, '..', '..', 'node_modules/mithril/package.json'), 'utf8')).version;
const mithrilPatchFileContentAsLines = fs.readFileSync(path.join(__dirname, '..', '..', 'spec/webpack/patches/mithril.js'), 'utf8').split("\n");

let isMithrilVersionLine = function (line) {
  return _.includes(line, 'm.version = ');
};

const mithrilVersionFromPatch = _.find(mithrilPatchFileContentAsLines, isMithrilVersionLine)
  .split("=")[1].trim().replace(/"/g, "");

if (mithrilVersionFromNpm !== mithrilVersionFromPatch) {
  throw new Error("Please ensure that the patched mithril version is the same as the one installed via nodejs");
}

const babelLoader = environment.loaders.get('babel');
babelLoader.test  = /\.(js|jsx|msx)?(\.erb)?$/;

console.log(`Generating assets in ${assetHost.path}`);
fsExtra.removeSync(assetHost.path);

environment.plugins.set('Provide',
  new webpack.ProvidePlugin({
    $:               "jquery",
    jQuery:          "jquery",
    "window.jQuery": "jquery"
  })
);

environment.plugins.set(
  'CommonsChunkVendor',
  new webpack.optimize.CommonsChunkPlugin({
    name:      'vendor-and-helpers.chunk',
    filename:  process.env.NODE_ENV == 'production' ? '[name]-[chunkhash].js' : '[name].js',
    minChunks: function (module, _count) {
      function isFromNPM() {
        return new RegExp(`node_modules`).test(module.resource);
      }

      function isInside() {
        return fs.realpathSync(module.resource).indexOf(fs.realpathSync(path.join(assetsDir, ..._(Array.prototype.concat.apply([], arguments)).flattenDeep().compact().value()))) === 0;
      }

      return module.resource && (isFromNPM() || isInside('helpers') || isInside('gen') || isInside('models', 'shared') || isInside('models', 'mixins') || isInside('views', 'shared'));
    },
  })
);

environment.plugins.set('StatsPlugin',
  new StatsPlugin('manifest-stats.json', {
    chunkModules: false,
    source:       false,
    chunks:       false,
    modules:      false,
    assets:       true
  })
);

environment.plugins.set('DefinePlugin',
  new webpack.DefinePlugin({'process.env': {NODE_ENV: JSON.stringify(process.env.NODE_ENV || 'development')}})
);

// run `yarn add --dev webpack-bundle-analyzer` to analyze dependencies and run with `--watch` arg
//const BundleAnalyzerPlugin = require('webpack-bundle-analyzer').BundleAnalyzerPlugin;
//environment.plugins.set('BundleAnalyzerPlugin', new BundleAnalyzerPlugin({analyzerPort: 9999}));

module.exports = environment;
