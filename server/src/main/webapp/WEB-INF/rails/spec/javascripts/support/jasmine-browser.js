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

module.exports = {
  srcDir: 'public/assets',
  srcFiles: [
    'application-*.js',
    'lib/d3-*.js',
  ],
  specDir: '.',
  specFiles: [
    'spec/javascripts/**/*[sS]pec.?(m)js',
  ],
  helpers: [
    'node_modules/jasmine-jquery/lib/jasmine-jquery.js',
    'node_modules/jasmine-ajax/lib/mock-ajax.js',
  ],
  env: {
    'stopSpecOnExpectationFailure': false,
    'stopOnSpecFailure': false,
    'random': false,
  },
  browser: {
    name: 'chrome' === process.env.BROWSER ? "headlessChrome" : 'headlessFirefox',
  }
};
