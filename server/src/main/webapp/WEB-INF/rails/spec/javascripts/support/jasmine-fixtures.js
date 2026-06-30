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

/*
 * Minimal, dependency-free replacement for the `setFixtures()` helper that
 * jasmine-jquery used to provide. It injects markup into a known container in
 * the document body and removes it automatically after every spec, so tests
 * start from a clean DOM.
 */
(function (global) {
  "use strict";

  const CONTAINER_ID = 'jasmine-fixtures';

  function cleanUp() {
    const container = global.document.getElementById(CONTAINER_ID);
    if (container) {
      container.remove();
    }
  }

  // Accepts an HTML string, a DOM node, or a jQuery object (matching the
  // signatures the old jasmine-jquery helper was called with).
  function setFixtures(html) {
    cleanUp();

    const container = global.document.createElement('div');
    container.id = CONTAINER_ID;

    if (typeof html === 'string') {
      container.innerHTML = html;
    } else if (global.jQuery && html instanceof global.jQuery) {
      html.appendTo(container);
    } else if (html instanceof global.Node) {
      container.appendChild(html);
    } else if (html != null) {
      container.innerHTML = String(html);
    }

    global.document.body.appendChild(container);
    return container;
  }

  global.setFixtures = setFixtures;

  // Always tidy up, regardless of whether a spec called setFixtures().
  afterEach(cleanUp);
})(typeof window !== 'undefined' ? window : this);
