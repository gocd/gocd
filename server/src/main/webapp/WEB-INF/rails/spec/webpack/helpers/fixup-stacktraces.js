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
/* eslint-disable no-var, prefer-arrow-callback, object-shorthand, prefer-template */

// Fixup stack traces, using this approach: https://gist.github.com/guncha/f45ceef6d483c384290a
import sourceMappedStackTrace from "sourcemapped-stacktrace";


jasmine.getEnv().addReporter({
  jasmineDone () {
    if (window.location.search.indexOf('unmangle=true') >= 0) {
      return;
    }
    const traces = document.querySelectorAll('.jasmine-stack-trace');
    for (let i = 0; i < traces.length; i++) {
      fixStackTrace(traces[i]);
    }
  }
});

function fixStackTrace(node) {
  sourceMappedStackTrace.mapStackTrace(node.textContent, (stack) => {
    const prevNode     = node.previousSibling;
    const prevNodeText = prevNode.getAttribute('title') || prevNode.textContent;
    node.textContent = `${prevNodeText}\n${stack.join('\n')}`;
  });
}
