/*
 * Copyright 2019 ThoughtWorks, Inc.
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

const merge                = require('webpack-merge');
const path                 = require('path');
const fsExtra              = require('fs-extra');
const webpack              = require('webpack');
const baseConfigFn         = require('./webpack-base.config.js');
const MiniCssExtractPlugin = require("mini-css-extract-plugin");

module.exports = function (env) {
  const baseConfig = baseConfigFn(env);
  const assetsDir  = path.join(__dirname, '..', 'webpack');
  const config     = {
    mode:    'production',
    devtool: "source-map",
    resolve: {
      modules: [assetsDir, 'node_modules']
    },
    output:  {
      filename: '[name]-[chunkhash].js'
    },
    plugins: [
      new MiniCssExtractPlugin({
        // Options similar to the same options in webpackOptions.output
        // both options are optional
        filename:      "[name]-[hash].css",
        chunkFilename: "[id]-[hash].css",
      }),
      new webpack.LoaderOptionsPlugin({minimize: true})
    ],
    module:  {
      rules: [
        {
          test: /\.(woff|woff2|ttf|eot|svg|png|gif|jpeg|jpg)(\?v=\d+\.\d+\.\d+)?$/,
          use:  [
            {
              loader:  'file-loader',
              options: {
                name:       '[name]-[hash].[ext]',
                outputPath: 'media/'
              }
            }
          ]
        },
      ]
    }
  };

  const mergedConfig = merge(baseConfig, config);
  fsExtra.removeSync(mergedConfig.output.path);
  return mergedConfig;
};
