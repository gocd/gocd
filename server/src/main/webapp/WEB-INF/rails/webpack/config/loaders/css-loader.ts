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

import {ConfigOptions} from "config/variables";
import MiniCssExtractPlugin from "mini-css-extract-plugin";
import path from "path";
import webpack from "webpack";

export function getCssLoaders(configOptions: ConfigOptions): webpack.RuleSetRule {
  return {
    test: /\.(s)?css$/,
    exclude: /node_modules/,
    use: [
      {
        loader: configOptions.production ? MiniCssExtractPlugin.loader : "style-loader",
        options: {
          esModule: false // we need CommonJS modules
        }
      },
      {
        loader: path.resolve(__dirname, "type-declarations-for-css-modules.ts"),
      },
      {
        loader: "css-loader",
        options: {
          esModule: false,
          modules: {
            mode: "local",
            localIdentName: "[name]__[local]___[hash:base64:5]",
            exportLocalsConvention: "camel-case",
            exportOnlyLocals: false
          },
          sourceMap: true,
          importLoaders: 1
        }
      },
      {
        loader: "sass-loader", // compiles Sass to CSS, defaults to Dart Sass
        options: {
          sourceMap: true,
          implementation: 'sass-embedded',
          sassOptions: { // Also see rails/config/application.rb for similar config for Sprockets
            quietDeps: true, // Mainly noise from FontAwesome and/or Bourbon left
            silenceDeprecations: [
              'import', // Can't do much about this until FontAwesome updates
              'legacy-js-api', // Need to move away from Webpack 4
            ],
          }
        }
      }
    ]
  };
}
