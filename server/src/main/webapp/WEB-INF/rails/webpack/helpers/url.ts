/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import m from "mithril";

/**
 * Retrieves a query parameter by name and converts the value to a string
 *
 * @param search: the search query string; this is generally `window.location.search`
 * @param name: the name of the query param to retrieve
 *
 * @returns the query param value as a string; if not present or null, returns an empty string
 */
export function queryParamAsString(search: string, name: string): string {
  // this can be a number of things (number, boolean, etc) so cannot just check for falsey-ness
  const value = m.parseQueryString(search)[name];

  if (null === value || void 0 === value) {
    return "";
  }

  if ("string" === typeof value) {
    return value;
  }

  return JSON.stringify(value);
}
