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

import fsExtra from "fs-extra";
import _ from "lodash";
import webpack from "webpack";
import {loaders} from "./loaders";
import {plugins} from "./plugins";
import {getEntries, getModules, outputDir} from "./variables";

const SpeedMeasurePlugin = require("speed-measure-webpack-plugin");
const TerserPlugin       = require("terser-webpack-plugin");

function configuration(env: any, argv: any): webpack.Configuration {
  env = _.assign({}, env);

  const production     = argv.mode === "production";
  const watch: boolean = !!argv.watch;

  if (production) {
    fsExtra.removeSync(outputDir);
  }

  const optimization: webpack.Options.Optimization = {
    splitChunks: {
      cacheGroups: {
        vendor: {
          name: "vendor-and-helpers.chunk",
          chunks: "all",
          minChunks: 2
        }
      }
    }
  };

  if (production) {
    optimization.minimizer = [new TerserPlugin()];
  }

  return {
    entry: getEntries(production),
    output: {
      path: outputDir,
      publicPath: "/go/assets/webpack/",
      filename: production ? "[name]-[chunkhash].js" : "[name].js"
    },
    cache: true,
    bail: true,
    devtool: production ? "source-map" : "inline-source-map",
    optimization,
    resolve: {
      extensions: [".js", ".js.msx", ".msx", ".tsx", ".ts"],
      modules: getModules(production),
    },
    module: {
      rules: loaders(watch, production)
    },
    plugins: plugins(watch, production),
  };
}

const smp = new SpeedMeasurePlugin();

export default smp.wrap(configuration);
