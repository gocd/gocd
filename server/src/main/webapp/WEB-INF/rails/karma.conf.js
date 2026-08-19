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

import jasmineSeedReporter from 'karma-jasmine-seed-reporter';
import path from 'node:path';
import process from 'node:process';
import manifest from './public/assets/webpack/manifest.json' with {type: 'json'};

let browsers;

if (process.platform === 'darwin') {
  browsers = ['FirefoxHeadless', 'ChromeHeadless'];
} else if (process.platform === 'win32') {
  // chrome is the ONLY browser (not even Chromium Edge!) that
  // works (or even installs successfully) on Windows Server Core
  browsers = ['ChromeHeadless'];
} else {
  // linux, bsd, et.al.
  browsers = ['FirefoxHeadless'];
}

// Workaround for lack of properly mapping assets to files; and use of dummy URLs in specs
function stubAssets() {
  return (req, res, next) => {
    if (req.url.startsWith('/go/api') || req.url.startsWith('/go/assets')) {
      res.writeHead(204);
      return res.end();
    }
    next();
  };
}

export default function (config) {
  config.set({
    basePath: 'public/assets/webpack',
    frameworks: ['jasmine'],
    middleware: ['stubAssets'],
    client: {
      captureConsole: true,
      jasmine: {
        random: true,
        seed: process.env['JASMINE_SEED']
      }
    },
    plugins: [
      jasmineSeedReporter,
      "karma-*",
      { 'middleware:stubAssets': ['factory', stubAssets] }
    ],
    preprocessors: {
      '**/*.js': ['sourcemap']
    },
    files: manifest.entrypoints.specRoot.assets.js.map((eachAsset) => {
      return {
        pattern: eachAsset.replace("/go/assets/webpack/", ""),
        watched: true,
        included: true,
        served: true
      };
    }),
    reporters: ['kjhtml', 'html', 'jasmine-seed'].concat(process.env['CI'] === 'false' ? ['progress'] : []),
    htmlReporter: {
      outputDir: path.resolve('../../../../../target/karma_reports'),
      focusOnFailures: true,
      namedFiles: false,
      urlFriendlyName: true,
      foldAll: false
    },
    port: 9876,
    colors: true,
    logLevel: config.LOG_INFO,
    autoWatch: true,
    browsers,
    singleRun: false,
    concurrency: Infinity
  });
};
