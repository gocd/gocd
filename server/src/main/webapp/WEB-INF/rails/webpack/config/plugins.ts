/*
 * Copyright Thoughtworks, Inc.
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

import {Buffer} from "buffer";
import ESLintPlugin from "eslint-webpack-plugin";
import ForkTsCheckerWebpackPlugin from "fork-ts-checker-webpack-plugin";
import fs from "fs";
import HtmlWebpackPlugin from "html-webpack-plugin";
import _ from "lodash";
import MiniCssExtractPlugin from "mini-css-extract-plugin";
import path from "path";
import webpack from "webpack";
import {WebpackAssetsManifest} from "webpack-assets-manifest";
import {ConfigOptions, getEntries} from "./variables";
import {LicensePlugins} from "./webpack-license-plugin";

const jasmineCore           = require("jasmine-core");
const StylelintPlugin       = require("stylelint-webpack-plugin");

export function plugins(configOptions: ConfigOptions): any[] {
  const plugins = [
    new ESLintPlugin({
      extensions: ["js", "msx"],
      exclude: ["node_modules", "webpack/gen"],
      failOnWarning: true,
      threads: false
    }),
    new StylelintPlugin({configFile: path.join(configOptions.railsRoot, ".stylelintrc.yml"), files: configOptions.assetsDir, failOnWarning: true}),
    new WebpackAssetsManifest({
      output: "manifest.json",
      entrypoints: true,
      writeToDisk: true,
      publicPath: true,
    }),
    new webpack.ProvidePlugin({
                                "$": "jquery",
                                "jQuery": "jquery",
                                "window.jQuery": "jquery"
                              }),
    new LicensePlugins(configOptions.licenseReportFile),
    new ForkTsCheckerWebpackPlugin({
      typescript: { memoryLimit: 800, diagnosticOptions: { semantic: true, syntactic: true } }
    })
  ];

  if (configOptions.production) {
    plugins.push(new MiniCssExtractPlugin({
                                            filename: "[name]-[contenthash].css",
                                            chunkFilename: "[id]-[contenthash].css",
                                            ignoreOrder: true
                                          }));
  } else {
    const jasmineFiles = jasmineCore.files;

    const entries = getEntries(configOptions);

    const jasmineIndexPage = {
      inject: true,
      xhtml: true,
      filename: "_specRunner.html",
      template: path.join(configOptions.railsRoot, "spec", "webpack", "_specRunner.html.ejs"),
      jasmineJsFiles: _.map(jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles), (file) => {
        return `__jasmine/${file}`;
      }),
      jasmineCssFiles: _.map(jasmineFiles.cssFiles, (file) => {
        return `__jasmine/${file}`;
      }),
      excludeChunks: _.keys(entries)
    };

    class JasmineAssetsPlugin {
      apply(compiler: webpack.Compiler) {
        compiler.hooks.emit.tapAsync("JasmineAssetsPlugin",
                                     (compilation: webpack.Compilation, callback: () => any) => {
                                       const allJasmineAssets = jasmineFiles.jsFiles.concat(jasmineFiles.bootFiles)
                                                                            .concat(jasmineFiles.cssFiles);

                                       _.each(allJasmineAssets, (asset) => {
                                         const file = path.join(jasmineFiles.path, asset);

                                         const contents = fs.readFileSync(file).toString();

                                         compilation.assets[`__jasmine/${asset}`] = {
                                           source() {
                                             return contents;
                                           },
                                           size() {
                                             return contents.length;
                                           },
                                           map() {
                                             return null;
                                           },
                                           sourceAndMap() {
                                             return {source: contents, map: null };
                                           },
                                           updateHash() {
                                             // no-op
                                           },
                                           buffer() {
                                             return Buffer.from(contents, "utf8");
                                           }
                                         };
                                       });

                                       callback();
                                     });
      }
    }

    plugins.push(new HtmlWebpackPlugin(jasmineIndexPage));
    plugins.push(new JasmineAssetsPlugin());
  }
  return plugins;
}
