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

  console.log(`Generating assets in ${outputDir}`); //eslint-disable-line no-console

  const entries = _.reduce(fs.readdirSync(singlePageAppModuleDir), (memo, file) => {
    const fileName   = path.basename(file);
    const moduleName = `single_page_apps/${_.split(fileName, '.')[0]}`;
    memo[moduleName] = path.join(singlePageAppModuleDir, file);
    return memo;
  }, {});

  const assetsDir = path.join(__dirname, '..', 'webpack');

  const plugins = [];
  plugins.push(new HappyPack({
    loaders: [
      {
        loader:  'babel-loader',
        options: {
          cacheDirectory: path.join(__dirname, '..', 'tmp', 'babel-loader')
        }
      }
    ],
    threads: 4
  }));
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
  plugins.push(new MiniCssExtractPlugin());

  if (production) {
    plugins.push(new webpack.LoaderOptionsPlugin({minimize: true}));
  }

  const config = {
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
    plugins,
    module:    {
      rules: [
        {
          test:    /\.(msx|js)$/,
          exclude: /node_modules/,
          use:     'happypack/loader',
        },
        { //TODO can this me moved to Happy?
          test:    /\.scss$/,
          exclude: /node_modules/,
          use:     [
            "style-loader",
            "css-loader?modules&importLoaders=1&localIdentName=[name]__[local]___[hash:base64:5]", // translates CSS into CommonJS
            "sass-loader" // compiles Sass to CSS, using Node Sass by default
          ]
        }
      ]
    }
  };

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

  config.plugins.push(new LicensePlugin());

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

  if (production) {
    config.mode = 'production';
    fsExtra.removeSync(config.output.path);
    config.devtool      = "source-map";
    config.optimization = {
      splitChunks: splitChunks
    };
  } else {
    config.mode    = 'development';
    config.devtool = "inline-source-map";

    config.resolve.modules.push(path.join(__dirname, 'spec', 'webpack'));

    config.optimization = {
      splitChunks: splitChunks
    };

    const jasmineCore  = require('jasmine-core');
    const jasmineFiles = jasmineCore.files;

    const HtmlWebpackPlugin = require('html-webpack-plugin');

    const jasmineIndexPage = {
      inject:          true,
      xhtml:           true,
      filename:        '_specRunner.html',
      template:        path.join(__dirname, '..', 'spec', 'webpack', '_specRunner.html.ejs'),
      jasmineJsFiles:  _.map(jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles), (file) => {
        return `__jasmine/${file}`;
      }),
      jasmineCssFiles: _.map(jasmineFiles.cssFiles, (file) => {
        return `__jasmine/${file}`;
      }),
      excludeChunks:   _.keys(entries)
    };

    config.plugins.push(new HtmlWebpackPlugin(jasmineIndexPage));

    config.entry['specRoot'] = path.join(__dirname, '..', 'spec', 'webpack', 'specRoot.js');

    const JasmineAssetsPlugin = function (_options) {
      this.apply = function (compiler) {
        compiler.plugin('emit', (compilation, callback) => {
          const allJasmineAssets = jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles).concat(jasmineFiles.cssFiles);

          _.each(allJasmineAssets, (asset) => {
            const file = path.join(jasmineFiles.path, asset);

            const contents = fs.readFileSync(file).toString();

            compilation.assets[`__jasmine/${asset}`] = {
              source() {
                return contents;
              },
              size() {
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
