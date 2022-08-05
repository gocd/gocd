/*
 * Copyright 2022 Thoughtworks, Inc.
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
 (function () {
  "use strict";

  // This file is shared between new and old pages, so we can't use ES6 syntax as the file
  // isn't guaranteed to be compiled

  if ("function" !== typeof Promise) {
    return;
  }

  // FIXME This test always succeeds, so we always apply the polyfill and override default browser behaviour
  if ("function" !== Promise.prototype.finally) {
    // OK, this is a cheap polyfill that is *mostly* conforming, but there
    // are some differences from the native `finally()`. See here:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/finally#Description
    Promise.prototype.finally = function(callback) {
      function invokeIgnoringArgs() { if ("function" === typeof callback) { callback(); } }
      return this.then(invokeIgnoringArgs, invokeIgnoringArgs);
    };
  }
})();
