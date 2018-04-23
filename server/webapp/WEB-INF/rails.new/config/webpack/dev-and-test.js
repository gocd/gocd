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

const environment = require('./environment');

const path              = require('path');
const fs                = require('fs');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const jasmineCore       = require('jasmine-core');
const _                 = require('lodash');

const jasmineFiles     = jasmineCore.files;
const jasmineIndexPage = {
  inject:          true,
  xhtml:           true,
  filename:        '_specRunner.html',
  template:        path.join(__dirname, '..', '..', 'spec', 'webpack', '_specRunner.html.ejs'),
  jasmineJsFiles:  _.map(jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles), function (file) {
    return '__jasmine/' + file;
  }),
  jasmineCssFiles: _.map(jasmineFiles.cssFiles, function (file) {
    return '__jasmine/' + file;
  }),
  //excludeChunks:   _.keys(entries)
};

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

environment.plugins.set('HtmlWebpackPlugin', new HtmlWebpackPlugin(jasmineIndexPage));
environment.plugins.set('JasmineAssetsPlugin', new JasmineAssetsPlugin());

module.exports = {
  entry:   {
    specRoot: path.join(__dirname, '..', '..', 'spec', 'webpack', 'specRoot.js')
  },
  resolve: {
    modules: [
      path.join(__dirname, '..', '..', 'spec', 'webpack', 'patches')
    ]
  }
};
