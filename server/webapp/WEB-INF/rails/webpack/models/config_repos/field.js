/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const _ = require("lodash");

function Field(name, options={}) {
  const DEFAULTS = {display: _.startCase(name), type: "text", default: "", required: true, readOnly: false};
  options = _.assign({}, DEFAULTS, options);

  this.keys = (this.keys || []);

  if (contains(this.keys, name)) {
    this.keys.push(name);
  } else {
    console.warn(`The key ${name} has already been defined on this object`); // eslint-disable-line no-console
  }

  let value;

  function attr(val) {
    if (arguments.length) {
      value = normalize(val, options);
    }

    return value;
  }

  this[name] = attr;

  attr.init = function (val) {
    if (options.type === "boolean") {
      return attr("boolean" === typeof val ? val : options.default);
    }

    if ("undefined" === typeof val || null === val) {
      val = options.default;
    }

    return attr(`${val}`);
  };

  attr.opts = (key) => options[key];
}

function contains(arr, el) { return !~arr.indexOf(el); }

function normalize(val, options) {
  if ("boolean" === options.type) { return !!val; }
  return ("string" === typeof val ? val : String(val || "")).trim();
}

module.exports = Field;
