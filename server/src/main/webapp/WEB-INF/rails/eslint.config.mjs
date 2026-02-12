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

import {defineConfig, globalIgnores} from "eslint/config";
import globals from "globals";
import js from "@eslint/js";
import {nonWebPackedLegacyConfig} from "./eslint-old.config.mjs";
import {webpackedConfig} from "./eslint-webpacked.config.mjs";

const globalIgnoreConfig = globalIgnores([
  "gems",                       // Gems
  "tmp",                        // Temporary typescript output dir
  "public",                     // Webpack/sprockets output dir
  "webpack/gen/**/*",           // Generated code

  ".yarn",                      // Vendored Yarn JS
  "node-vendor",                // Vendored webpacked JS
  "app/assets/javascripts/lib", // Vendored non-webpacked JS
]);

// Fallback config for build script configurations
const buildScriptingConfig = {
  files: ["*.{js,mjs,ts}"],
  ignores: [
    "app/**/*",     // Non-webpacked JS
    "webpack/**/*", // Webpacked JS linted separately
    "spec/**/*",    // Webpacked JS linted separately
  ],
  extends: [js.configs.recommended],
  languageOptions: {
    globals: {
      ...globals.node,
      module: true,
    },
    ecmaVersion: "latest",
    sourceType: "module",
  },
};

export default defineConfig([
  globalIgnoreConfig,
  buildScriptingConfig,
  webpackedConfig,
  nonWebPackedLegacyConfig
]);
