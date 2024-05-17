/*
 * Copyright 2024 Thoughtworks, Inc.
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

import _ from "lodash";
import path from "path";
import webpack from "webpack";
import {loaders} from "./loaders";
import {plugins} from "./plugins";
import {ConfigOptions, getEntries, getModules} from "./variables";

function getConfigOptions(argv: any, env: any): ConfigOptions {
  const assetsDir              = path.join(__dirname, "..");
  const railsRoot              = path.join(assetsDir, "..");
  const production             = argv.mode === "production";
  const watch                  = argv.watch;
  const singlePageAppModuleDir = path.join(assetsDir, "single_page_apps");
  const cacheDir               = path.join(railsRoot, "tmp");
  const outputDir              = env.outputDir || path.join(railsRoot, "public", "assets", "webpack");
  const licenseReportFile      = env.licenseReportFile || path.join(railsRoot,
                                                                    "yarn-license-report",
                                                                    `used-packages-${production ? "prod" : "dev"}.json`);
  return {
    production,
    watch,
    assetsDir,
    singlePageAppModuleDir,
    railsRoot,
    cacheDir,
    outputDir,
    licenseReportFile
  };
}

function getOptimization(configOptions: ConfigOptions): any {
  return configOptions.production ? {
    splitChunks: {
      cacheGroups: {
        vendor: {
          name: "vendor-and-helpers.chunk",
          chunks: "all",
          minChunks: 2
        }
      }
    }
  } : {
    splitChunks: {
      chunks: "all",
      minSize: 100_000
    }
  };
}

function configuration(env: any, argv: any): webpack.Configuration {
  env  = _.assign({}, env);
  argv = _.assign({}, argv);

  const configOptions = getConfigOptions(argv, env);
  const optimization  = getOptimization(configOptions);

  return {
    entry: getEntries(configOptions),
    output: {
      path: configOptions.outputDir,
      publicPath: "/go/assets/webpack/",
      filename: configOptions.production ? "[name]-[contenthash].js" : "[name].js"
    },
    cache: {
      type: 'filesystem',
      cacheDirectory: path.join(configOptions.cacheDir, "webpack-cache"),
    },
    bail: !argv.watch,
    devtool: configOptions.production ? "source-map" : "eval-source-map",
    optimization,
    resolve: {
      extensions: [".js", ".js.msx", ".msx", ".tsx", ".ts"],
      modules: getModules(configOptions),
    },
    module: {
      rules: loaders(configOptions)
    },
    plugins: plugins(configOptions),
  };
}
export default configuration;
