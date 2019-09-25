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

import {CleanWebpackPlugin} from "clean-webpack-plugin";
import ForkTsCheckerWebpackPlugin from "fork-ts-checker-webpack-plugin";
import fs from "fs";
import HtmlWebpackPlugin from "html-webpack-plugin";
import _ from "lodash";
import MiniCssExtractPlugin from "mini-css-extract-plugin";
import path from "path";
import webpack from "webpack";
import {ConfigOptions, getEntries} from "./variables";
import {LicensePlugins} from "./webpack-license-plugin";

const jasmineCore                = require("jasmine-core");
const StatsPlugin                = require("stats-webpack-plugin");
const SassLintPlugin             = require("sass-lint-webpack");
const UnusedFilesWebpackPlugin   = require("unused-files-webpack-plugin").default;
const WebpackBuildNotifierPlugin = require("webpack-build-notifier");

export function plugins(configOptions: ConfigOptions): webpack.Plugin[] {
  const plugins = [
    new CleanWebpackPlugin(),
    new UnusedFilesWebpackPlugin({
                                   patterns: ["webpack/**/*.*", "spec/webpack/**/*.*"],
                                   globOptions: {
                                     ignore: ["node_modules/**/*.*", "tmp/**/*", "**/tsconfig.json", "webpack/config/**/*.*"],
                                   }
                                 }) as webpack.Plugin,
    new SassLintPlugin({configPath: path.join(configOptions.assetsDir, ".sasslintrc")}) as webpack.Plugin,
    new StatsPlugin("manifest.json", {
      chunkModules: false,
      source: false,
      chunks: false,
      modules: false,
      assets: true
    }) as webpack.Plugin,
    new webpack.ProvidePlugin({
                                "$": "jquery",
                                "jQuery": "jquery",
                                "window.jQuery": "jquery"
                              }) as webpack.Plugin,
    new LicensePlugins(configOptions.licenseReportFile),
    new ForkTsCheckerWebpackPlugin({
                                     checkSyntacticErrors: true,
                                     useTypescriptIncrementalApi: true,
                                   })
  ];

  if (configOptions.production) {
    plugins.push(new MiniCssExtractPlugin({
                                            // Options similar to the same options in webpackOptions.output
                                            // both options are optional
                                            filename: "[name]-[hash].css",
                                            chunkFilename: "[id]-[hash].css",
                                            ignoreOrder: true
                                          }));
  } else {
    const jasmineFiles = jasmineCore.files;

    const entries = getEntries(configOptions);
    delete entries.specRoot;

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
                                     (compilation: webpack.compilation.Compilation, callback: () => any) => {
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
                                           }
                                         };
                                       });

                                       callback();
                                     });
      }
    }

    plugins.push(new HtmlWebpackPlugin(jasmineIndexPage));
    plugins.push(new JasmineAssetsPlugin());
    plugins.push(new WebpackBuildNotifierPlugin({
                                                  suppressSuccess: true,
                                                  suppressWarning: true
                                                })
    );
  }
  return plugins;
}
