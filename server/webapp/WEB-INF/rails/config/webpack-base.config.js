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

/* global __dirname, console */

'use strict';

const fs                         = require('fs');
const _                          = require('lodash');
const path                       = require('path');
const webpack                    = require('webpack');
const StatsPlugin                = require('stats-webpack-plugin');
const LicenseChecker             = require('./webpack-license-plugin');
const SassLintPlugin             = require('sass-lint-webpack');
const CheckerPlugin              = require('awesome-typescript-loader').CheckerPlugin;
const MiniCssExtractPlugin       = require("mini-css-extract-plugin");
const {UnusedFilesWebpackPlugin} = require("unused-files-webpack-plugin");

const singlePageAppModuleDir = path.join(__dirname, '..', 'webpack', 'single_page_apps');

const mithrilVersionFromNpm          = JSON.parse(fs.readFileSync(path.join(__dirname, '..', 'node_modules/mithril/package.json'), 'utf8')).version;
const mithrilPatchFileContentAsLines = fs.readFileSync(path.join(__dirname, '..', 'spec/webpack/patches/mithril.js'), 'utf8').split("\n");

const isMithrilVersionLine = function (line) {
  return _.includes(line, 'm.version = ');
};

const mithrilVersionFromPatch = _.find(mithrilPatchFileContentAsLines, isMithrilVersionLine)
  .split("=")[1].trim().replace(/"/g, "");

if (mithrilVersionFromNpm !== mithrilVersionFromPatch) {
  throw new Error("Please ensure that the patched mithril version is the same as the one installed via nodejs");
}

module.exports = function (env) {
  const production        = (env && env.production === 'true');
  const outputDir         = (env && env.outputDir) || path.join(__dirname, '..', 'public', 'assets', 'webpack');
  const licenseReportFile = (env && env.licenseReportFile) || path.join(__dirname, '..', 'yarn-license-report', `used-packages-${production ? 'prod' : 'dev'}.json`);
  const assetsDir         = path.join(__dirname, '..', 'webpack');
  console.log(`Generating assets in ${outputDir}`); //eslint-disable-line no-console

  const generateConfig = function (entries, splitChunks, plugins) {
    return {
      cache:        true,
      bail:         true,
      entry:        entries,
      output:       {
        path:       outputDir,
        publicPath: '/go/assets/webpack/',
      },
      resolve:      {
        extensions: ['.js', '.js.msx', '.msx', '.es6', '.tsx', '.ts'],
        alias:      {
          'string-plus': 'helpers/string-plus',
          'string':      'underscore.string',
          'jQuery':      'jquery',
        }
      },
      optimization: {
        splitChunks
      },
      devServer:    {
        hot:    false,
        inline: false
      },
      plugins,
      module:       {
        rules: [
          {
            enforce: "pre",
            test:    /\.(js|msx)$/,
            exclude: [
              /node_modules/,
              /js-routes\.js$/
            ]
            ,
            loader:  "eslint-loader"
          },
          {
            test:    /\.ts(x)?$/,
            exclude: /node_modules/,
            use:     [
              {
                loader:  'babel-loader',
                options: {
                  cacheDirectory: path.join(__dirname, '..', 'tmp', 'babel-loader')
                }
              },
              {
                loader:  'awesome-typescript-loader',
                options: {
                  configFileName: path.join(__dirname, '..', 'tsconfig.common.json')
                }
              },
            ]
          },
          {
            test:    /\.(msx|js)$/,
            exclude: /node_modules/,
            use:     [{
              loader:  'babel-loader',
              options: {
                cacheDirectory: path.join(__dirname, '..', 'tmp', 'babel-loader')
              }
            }]
          },
          {
            test:    /\.(s)?css$/,
            exclude: /node_modules/,
            use:     [
              {
                loader: production ? MiniCssExtractPlugin.loader : 'style-loader',
              },
              {
                loader:  "typings-for-css-modules-loader", // translates CSS into CommonJS
                options: {
                  modules:        true,
                  sourceMap:      true,
                  camelCase:      true,
                  importLoaders:  1,
                  localIdentName: "[name]__[local]___[hash:base64:5]",
                  namedExport:    true,
                  banner:         "// This file is automatically generated by typings-for-css-modules.\n// Please do not change this file!"
                }
              },
              {
                loader:  "sass-loader", // compiles Sass to CSS, using Node Sass by default
                options: {
                  sourceMap: true
                }
              },
              {
                loader: path.resolve(__dirname, 'custom-loader')
              }
            ]
          },
        ]
      }
    };
  };

  const entries = _.reduce(fs.readdirSync(singlePageAppModuleDir), (memo, file) => {
    const fileName   = path.basename(file);
    const moduleName = `single_page_apps/${_.split(fileName, '.')[0]}`;
    memo[moduleName] = path.join(singlePageAppModuleDir, file);
    return memo;
  }, {});

  const plugins = [
    new UnusedFilesWebpackPlugin({
      patterns:    ['webpack/**/*.*', 'spec/webpack/**/*.*'],
      globOptions: {
        ignore: ['node_modules/**/*.*', 'tmp/**/*', '**/tsconfig.json'],
      }
    }),
    new CheckerPlugin(),
    new SassLintPlugin(),
    new StatsPlugin('manifest.json', {
      chunkModules: false,
      source:       false,
      chunks:       false,
      modules:      false,
      assets:       true
    }),
    new webpack.ProvidePlugin({
      $:               "jquery",
      jQuery:          "jquery",
      "window.jQuery": "jquery"
    }),
    new LicenseChecker({licenseReportFile})
  ];

  const splitChunks = {
    cacheGroups: {
      default: false,
      vendors: false,
      vendor:  {
        name:   'vendor-and-helpers.chunk',
        chunks: 'all',
        test(module, _chunks) {
          function isFromNPM() {
            const name = module.nameForCondition && module.nameForCondition();
            return new RegExp(`node_modules`).test(name) || new RegExp(`node-vendor`).test(name);
          }

          function isInside() {
            return module.resource.indexOf(path.join(assetsDir, ..._(Array.prototype.concat.apply([], arguments)).flattenDeep().compact().value())) === 0;
          }

          return module.resource && (isFromNPM() || isInside('helpers') || isInside('gen') || isInside('models', 'shared') || isInside('models', 'mixins') || isInside('views', 'shared'));
        }
      }
    }
  };

  return generateConfig(entries, splitChunks, plugins);
};
