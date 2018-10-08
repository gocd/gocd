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

const fs                   = require('fs');
const fsExtra              = require('fs-extra');
const _                    = require('lodash');
const path                 = require('path');
const upath                = require('upath');
const webpack              = require('webpack');
const StatsPlugin          = require('stats-webpack-plugin');
const HappyPack            = require('happypack');
const licenseChecker       = require('license-checker');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

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
  const happyThreadPool   = HappyPack.ThreadPool({size: 4});
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
        extensions: ['.js', '.js.msx', '.msx', '.es6'],
        alias:      {
          'string-plus': 'helpers/string-plus',
          'string':      'underscore.string',
          'jQuery':      'jquery',
        }
      },
      optimization: {
        splitChunks: splitChunks
      },
      devServer:    {
        hot:    false,
        inline: false
      },
      plugins,
      module:       {
        rules: [
          {
            test:    /\.(msx|js)$/,
            exclude: /node_modules/,
            use:     'happypack/loader?id=jsx',
          },
          {
            test:    /\.scss$/,
            exclude: /node_modules/,
            use:     [
              'happypack/loader?id=scss'
            ]
          },
          {
            test: /\.woff(2)?(\?v=[0-9]\.[0-9]\.[0-9])?$/,
            use:  'happypack/loader?id=woff'
          },
          {
            test: /\.(ttf|eot|svg)(\?v=[0-9]\.[0-9]\.[0-9])?$/,
            use:  [ //happypack doesn't work with file-loader as of now
              {loader: 'file-loader'}
            ]
          }
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

  const LicensePlugin = function (_options) {
    this.apply = function (compiler) {
      licenseChecker.init({json: true, production: false, start: process.cwd()}, (err, licenseData) => {
        if (err) {
          console.error('Found error');
          console.error(err);
          process.exit(1);
        }

        const filenames = [];
        compiler.plugin("emit", (compilation, callback) => {
          compilation.chunks.forEach((chunk) => {
            chunk.modulesIterable.forEach((chunkModule) => {
              filenames.push(chunkModule.resource || (chunkModule.rootModule && chunkModule.rootModule.resource));
              if (Array.isArray(chunkModule.fileDependencies)) {
                chunkModule.fileDependencies.forEach((e) => {
                  filenames.push(e);
                });
              }
            });
          });
          callback();

          const licenseReport = _.chain(filenames)
            .uniq()
            .filter((fileName) => fileName && fileName.indexOf('node_modules') >= 0)
            .map((fileName) => {
              const file = upath.normalize(fileName).replace(upath.join(process.cwd(), '/node_modules/'), '');
              return file.startsWith("@") ? file.split("/").slice(0, 2).join("/") : file.split("/")[0];
            })
            .uniq()
            .sort()
            .reduce((accumulator, moduleName) => {
              const moduleNameWithVersion = _(licenseData).findKey((moduleLicenseInfo, moduleNameWithVersion) => {
                return moduleNameWithVersion.startsWith(`${moduleName}@`);
              });

              const licenseDataForModule = licenseData[moduleNameWithVersion];

              if (licenseDataForModule) {
                const moduleVersion = moduleNameWithVersion.split('@')[1];

                if (licenseDataForModule.licenseFile) {
                  const licenseReportDirForModule = path.join(path.dirname(licenseReportFile), `${moduleName}-${moduleVersion}`);
                  fsExtra.removeSync(licenseReportDirForModule);
                  fsExtra.mkdirsSync(licenseReportDirForModule);
                  fsExtra.copySync(licenseDataForModule.licenseFile, path.join(licenseReportDirForModule, path.basename(licenseDataForModule.licenseFile)));
                }

                accumulator[moduleName] = {
                  moduleName:     moduleName,
                  moduleVersion:  moduleVersion,
                  moduleUrls:     [licenseDataForModule.repository],
                  moduleLicenses: [
                    {
                      moduleLicense:    licenseDataForModule.licenses,
                      moduleLicenseUrl: `https://spdx.org/licenses/${licenseDataForModule.licenses}.html`
                    }
                  ]
                };
              } else {
                console.error(`Unable to find license data for ${moduleName}`);
                process.exit(1);
              }
              return accumulator;
            }, {}).value();

          fs.writeFileSync(licenseReportFile, `${JSON.stringify(licenseReport, null, 2)}\n`);
        });

      });
    };
  };

  const plugins = [
    new HappyPack({
      id:         'jsx',
      loaders:    [
        {
          loader:  'babel-loader',
          options: {
            cacheDirectory: path.join(__dirname, '..', 'tmp', 'babel-loader')
          }
        }
      ],
      threadPool: happyThreadPool
    }),
    new HappyPack({
      id:         'scss',
      loaders:    [
        {loader: 'style-loader'},
        {loader: "css-loader?modules&camelCase&importLoaders=1&localIdentName=[name]__[local]___[hash:base64:5]"}, // translates CSS into CommonJS
        {
          loader:  "sass-loader", // compiles Sass to CSS, using Node Sass by default
          options: {
            includePaths: [
              path.join(__dirname, '..', 'node_modules', 'font-awesome', 'scss')
            ]
          }
        }
      ],
      threadPool: happyThreadPool
    }),
    new HappyPack({
      id:         'woff',
      loaders:    [
        {
          loader:  'url-loader',
          options: {
            limit:    10000,
            mimetype: 'application/font-woff'
          }
        }],
      threadPool: happyThreadPool
    }),
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
    new LicensePlugin()
  ];

  const splitChunks = {
    cacheGroups: {
      default: false,
      vendors: false,
      vendor:  {
        name:   'vendor-and-helpers.chunk',
        chunks: 'all',
        test(module, chunks) {
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
