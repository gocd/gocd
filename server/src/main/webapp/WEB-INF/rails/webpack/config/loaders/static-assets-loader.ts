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

import {ConfigOptions} from "config/variables";
import webpack from "webpack";

export function getStaticAssetsLoader(configOptions: ConfigOptions): webpack.RuleSetRule {
  return {
    test: /\.(woff|woff2|ttf|eot|svg|png|gif|jpeg|jpg)(\?v=\d+\.\d+\.\d+)?$/,
    use: [
      {
        loader: "file-loader",
        options: {
          name: configOptions.production ? "[name]-[hash].[ext]" : "[name].[ext]",
          outputPath: configOptions.production ? "media/" : "fonts/"
        }
      }
    ]
  };
}
