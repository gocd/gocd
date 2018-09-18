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

// This is the starting point for booting up jasmine

function importAll(r) {
  r.keys().forEach(r);
}

// ensure that helpers are loaded before anything else
importAll(require.context('./helpers', true, /\.js$/));

importAll(require.context('./lib', true, /\.js$/));
importAll(require.context('./models', true, /\.js$/));
importAll(require.context('./views', true, /\.js$/));
