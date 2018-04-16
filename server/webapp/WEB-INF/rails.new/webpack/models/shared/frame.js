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

 (function() {
  "use strict";

  const Stream = require("mithril/stream");
  const      $ = require("jquery");
  const    esr = require("escape-string-regexp");
  const    enc = encodeURIComponent;

  function paramPresent(key, val) {
    const s = window.location.search,
         re = new RegExp("undefined" !== typeof val ?
            `[&?]${esr(enc(key))}=${esr(enc(val))}\\b` :
            `[&?]${esr(enc(key))}\\b`);
    return "" !== s && s.match(re);
  }

  function withParam(url, key, val) {
    const p = "undefined" !== typeof val ? `${enc(key)}=${enc(val)}` : enc(key);
    return url + (url.match(/[&?]/) ? "&" : "?") + p;
  }

  function passThruParams(url, params=[]) {
    for (let i = 0, len = params.length; i < len; ++i) {
      const key = params[i].key, val = params[i].val;
      url = paramPresent(key, val) ? withParam(url, key, val) : url;
    }
    return url;
  }

  function Frame(callback) {
    const url = Stream();
    const view = Stream();
    const data = Stream();
    const pluginId = Stream();
    const errors = Stream();

    function load() {
      errors(null);

      $.ajax({
        url: url(),
        type: "GET",
        dataType: "json"
      }).done((r) => {
        data(r.data);
        view(passThruParams(r.view_path, [{key: "ui", val: "test"}]));
      }).fail((xhr) => {
        errors(xhr);
      }).always(() => {
        callback();
      });
    }

    function fetch(url, handler) {
      errors(null);

      $.ajax({
        url,
        type: "GET",
        dataType: "json"
      }).done((r) => {
        handler(r.data, null);
      }).fail((xhr) => {
        errors(xhr);
        handler(null, errors());
      });
    }

    (Object.assign || $.extend)(this, {url, view, data, load, fetch, pluginId, errors});
  }

  module.exports = Frame;
})();
