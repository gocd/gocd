/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const merge                    = require('webpack-merge');
const fs                       = require('fs');
const _                        = require('lodash');
const path                     = require('path');
const jasmineCore              = require('jasmine-core');
const HtmlWebpackPlugin        = require('html-webpack-plugin');
var WebpackBuildNotifierPlugin = require('webpack-build-notifier');

const baseConfigFn = require('./webpack-base.config.js');

module.exports = function (env) {
  const baseConfig   = baseConfigFn(env);
  const assetsDir    = path.join(__dirname, '..', 'webpack');
  const jasmineFiles = jasmineCore.files;

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
    excludeChunks:   _.keys(baseConfig.entry)
  };

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

  const config = {
    mode:      'development',
    devtool:   'inline-source-map',
    entry:     {
      specRoot: path.join(__dirname, '..', 'spec', 'webpack', 'specRoot.js')
    },
    output:    {
      filename: '[name].js'
    },
    resolve:   {
      modules: [
        //order is important, otherwise monkey patches won't be applied
        path.join(__dirname, '..', 'spec', 'webpack', 'patches'), // provide monkey patches libs for tests
        path.join(__dirname, 'spec', 'webpack'),
        assetsDir,
        'node_modules'
      ],
    },
    plugins:   [
      new HtmlWebpackPlugin(jasmineIndexPage),
      new JasmineAssetsPlugin(),
      new WebpackBuildNotifierPlugin({
        suppressSuccess: true,
        suppressWarning: true
      })
    ],
    devServer: {
      hot:    true,
      inline: true
    },
    module:    {
      rules: [
        {
          test: /\.(woff(2)?|ttf|eot|svg)(\?v=\d+\.\d+\.\d+)?$/,
          use:  [
            {
              loader:  'file-loader',
              options: {
                name:       '[name].[ext]',
                outputPath: 'fonts/'
              }
            }
          ]
        },
      ]
    }
  };

  return merge(baseConfig, config);
};
