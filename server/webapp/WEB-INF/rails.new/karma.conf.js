/*
 * Copyright 2017 ThoughtWorks, Inc.
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

const path    = require('path');
const process = require('process');

let browsers;

if (process.platform === 'darwin') {
  browsers = ['Firefox', 'Chrome'];
} else if (process.platform === 'win32') {
  browsers = ['Firefox', 'IE'];
} else {
  // linux, bsd, et.al.
  browsers = ['Firefox'];
}

module.exports = function (config) {
  config.set({
    basePath:      path.join(__dirname, 'public', 'assets', 'webpack'),
    frameworks:    ['jasmine'],
    preprocessors: {
      '**/*.js': ['sourcemap']
    },
    files:         [
      {
        pattern:  'vendor-and-helpers.chunk.js',
        watched:  true,
        included: true,
        served:   true
      },
      {
        pattern:  'specRoot.js',
        watched:  true,
        included: true,
        served:   true
      },
    ],
    reporters:     ['progress', 'junit', 'kjhtml', 'html'],
    htmlReporter:  {
      outputDir:               path.join(__dirname, 'tmp', 'karma_reports'),
      templatePath:            null,
      focusOnFailures:         true,
      namedFiles:              false,
      pageTitle:               null,
      urlFriendlyName:         false,
      preserveDescribeNesting: false,
      foldAll:                 false,
    },
    junitReporter: {
      outputDir:          path.join(__dirname, 'tmp', 'karma_reports'),
      outputFile:         undefined,
      suite:              '',
      useBrowserName:     true,
      nameFormatter:      undefined,
      classNameFormatter: undefined,
      properties:         {}
    },
    port:          9876,
    colors:        true,
    logLevel:      process.env['KARMA_LOG_LEVEL'] ? config[`LOG_${process.env['KARMA_LOG_LEVEL'].toUpperCase()}`] : config.LOG_INFO,
    autoWatch:     true,
    browsers:      browsers,
    singleRun:     false,
    concurrency:   Infinity
  });
};
