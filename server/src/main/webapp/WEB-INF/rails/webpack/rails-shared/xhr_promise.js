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
 (function () {
  "use strict";

   var NotSupported = function() { // eslint-disable-line
     throw new Error("Native Promises are not supported in this browser.");
   };

  // This file is shared between new and old pages, so we can't use ES6 syntax as the file
  // isn't guaranteed to be compiled

  /* eslint-disable no-var,prefer-template,object-shorthand,prefer-arrow-callback */

  if ("function" !== typeof Promise) {


    if ("undefined" !== typeof module) {
      module.exports = NotSupported;
    } else {
      window.XhrPromise = NotSupported;
    }

    return;
  }

  if ("function" !== Promise.prototype.finally) {
    // OK, this is a cheap polyfill that is *mostly* conforming, but there
    // are some differences from the native `finally()`. See here:
    // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Promise/finally#Description
    Promise.prototype.finally = function(callback) {
      function invokeIgnoringArgs() { if ("function" === typeof callback) { callback(); } }
      return this.then(invokeIgnoringArgs, invokeIgnoringArgs);
    };
  }

  var XhrPromise = function(settings) {
    // Uses a native XMLHttpRequest object because jQuery XHR does not support
    // "blob" as a responseType (and doesn't provide a clean way to access the native
    // xhr object)
    return new Promise(function req(resolve, reject) {
      var options = Object.assign({
        type: "GET",
        responseType: "text",
        headers: {}
      }, settings);

      if ("undefined" === typeof options.url) {
        throw new Error("Please specify a `url` in your XhrPromise settings");
      }

      var xhr = new XMLHttpRequest();

      xhr.onreadystatechange = function onreadystatechange() {
        if (4 === xhr.readyState) { // request complete
          if (xhr.status < 400 && xhr.status > 199) {
            resolve({data: xhr.response, xhr: xhr});
          } else {
            reject({error: xhr.response, xhr: xhr});
          }
        }
      };

      xhr.open(options.type, options.url);
      xhr.responseType = options.responseType;

      for (var key in options.headers) {
        xhr.setRequestHeader(key, options.headers[key]);
      }

      if ("function" === typeof options.beforeSend) {
        options.beforeSend(xhr);
      }

      xhr.send();
    });
  };

  if ("undefined" !== typeof module) {
    module.exports = XhrPromise;
  } else {
    window.XhrPromise = XhrPromise;
  }
})();
