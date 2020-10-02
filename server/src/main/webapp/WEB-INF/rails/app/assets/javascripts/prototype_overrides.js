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

  var PrototypeOverrides = function PrototypeOverrides() {
    this.overrideJSONStringify = function () {
      var _json_stringify = JSON.stringify;
      JSON.stringify = function (value) {
        var _array_tojson = Array.prototype.toJSON;
        delete Array.prototype.toJSON;
        var r = _json_stringify(value);
        Array.prototype.toJSON = _array_tojson;
        return r;
      };
    };
  };

  if ("undefined" !== typeof module) {
    module.exports = PrototypeOverrides;
  } else {
    window.PrototypeOverrides = PrototypeOverrides;
  }
})();