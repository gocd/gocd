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
/* global __dirname */

import process from 'process';
import jasmineSeedReporter from 'karma-jasmine-seed-reporter';
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

export default function (config) {
  config.set({
    basePath: 'public/assets/webpack',
    frameworks: ['jasmine'],
    client: {
      captureConsole: true,
      jasmine: {
        random: true,
        seed: process.env['JASMINE_SEED']
      }
    },
    plugins: [jasmineSeedReporter, "karma-*"],
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
    reporters: ['progress', 'junit', 'kjhtml', 'html', 'jasmine-seed'],
    htmlReporter: {
      outputDir: '../../../../../target/karma_reports',
      templatePath: null,
      focusOnFailures: true,
      namedFiles: false,
      pageTitle: null,
      urlFriendlyName: false,
      preserveDescribeNesting: false,
      foldAll: false,
    },
    junitReporter: {
      outputDir: '../../../../../target/karma_reports',
      outputFile: undefined,
      suite: '',
      useBrowserName: true,
      nameFormatter: undefined,
      classNameFormatter: undefined,
      properties: {}
    },
    port: 9876,
    colors: true,
    logLevel: process.env['KARMA_LOG_LEVEL'] ? config[`LOG_${process.env['KARMA_LOG_LEVEL'].toUpperCase()}`] : config.LOG_INFO,
    autoWatch: true,
    browsers,
    singleRun: false,
    concurrency: Infinity
  });
};
