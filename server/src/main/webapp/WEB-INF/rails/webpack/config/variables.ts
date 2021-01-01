/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import fs from "fs";
import _ from "lodash";
import path from "path";
import webpack from "webpack";

export interface ConfigOptions {
  production: boolean;
  watch: boolean;
  assetsDir: string;
  singlePageAppModuleDir: string;
  railsRoot: string;
  tempDir: string;
  outputDir: string;
  licenseReportFile: string;
}

export function getModules(configOptions: ConfigOptions) {
  const modules = [configOptions.assetsDir, path.join(configOptions.railsRoot, "node_modules")];
  if (!configOptions.production) {
    modules.unshift(path.join(configOptions.railsRoot, "spec", "webpack"));
  }
  return modules;
}

export function getEntries(configOptions: ConfigOptions): webpack.Entry {
  const entries = _.reduce(fs.readdirSync(configOptions.singlePageAppModuleDir), (memo, file) => {
    const fileName   = path.basename(file);
    const moduleName = `single_page_apps/${_.split(fileName, ".")[0]}`;
    memo[moduleName] = path.join(configOptions.singlePageAppModuleDir, file);
    return memo;
  }, {} as webpack.Entry);

  if (!configOptions.production) {
    entries.specRoot = path.join(configOptions.railsRoot, "spec", "webpack", "specRoot.js");
  }
  return entries;
}
