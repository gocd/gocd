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

import _ from "lodash";
import path from "path";
import webpack from "webpack";

import fs = require("fs");

export const assetsDir              = path.join(__dirname, "..");
export const singlePageAppModuleDir = path.join(assetsDir, "single_page_apps");
export const railsRoot              = path.join(assetsDir, "..");
export const tempDir                = path.join(railsRoot, "tmp");
export const outputDir              = path.join(railsRoot, "public", "assets", "webpack");

export function getModules(production: boolean) {
  const modules = [assetsDir, path.join(railsRoot, "node_modules")];
  if (!production) {
    modules.unshift(path.join(railsRoot, "spec", "webpack"));
    modules.unshift(path.join(railsRoot, "spec", "webpack", "patches"));
  }
  return modules;
}

export function getEntries(production: boolean): webpack.Entry {
  const entries = _.reduce(fs.readdirSync(singlePageAppModuleDir), (memo, file) => {
    const fileName   = path.basename(file);
    const moduleName = `single_page_apps/${_.split(fileName, ".")[0]}`;
    memo[moduleName] = path.join(singlePageAppModuleDir, file);
    return memo;
  }, {} as webpack.Entry);

  if (!production) {
    entries.specRoot = path.join(railsRoot, "spec", "webpack", "specRoot.js");
  }
  return entries;
}
