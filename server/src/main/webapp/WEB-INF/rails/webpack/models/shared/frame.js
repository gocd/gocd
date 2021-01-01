/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import Stream from "mithril/stream";
import $ from "jquery";
import esr from "escape-string-regexp";

const enc = encodeURIComponent;

function paramPresent(win, key, val) {
  const s  = win.location.search,
        re = new RegExp("undefined" !== typeof val ?
          `[&?]${esr(enc(key))}=${esr(enc(val))}(?:&.+)?$` :
          `[&?]${esr(enc(key))}(?:&.+)?$`);
  return "" !== s && s.match(re);
}

function withParam(url, key, val) {
  const p = "undefined" !== typeof val ? `${enc(key)}=${enc(val)}` : enc(key);
  return url + (url.match(/[&?]/) ? "&" : "?") + p;
}

function passThruParams(win, url, params = []) {
  for (let i = 0, len = params.length; i < len; ++i) {
    const key = params[i].key, val = params[i].val;
    url       = paramPresent(win, key, val) ? withParam(url, key, val) : url;
  }
  return url;
}

export function Frame(self = window) {
  const url    = Stream();
  const view   = Stream();
  const data   = Stream();
  const errors = Stream();

  let nonce = 0;

  function load(before, after) {
    errors(null);
    $.ajax({
      url:        url(),
      type:       "GET",
      dataType:   "json",
      beforeSend: "function" === typeof before && before
    }).done((r) => {
      data(r.data);
      view(withParam(r.view_path, "~n.o.n.c.e", nonce++));
    }).fail((xhr) => {
      errors(xhr);
    }).always(() => {
      if ("function" === typeof after) {
        after();
      }
    });
  }

  function fetch(url, handler) {
    errors(null);

    $.ajax({
      url,
      type:     "GET",
      dataType: "json"
    }).done((r) => {
      handler(r.data, null);
    }).fail((xhr) => {
      errors(xhr);
      handler(null, errors());
    });
  }

  /** @private differs than view.map(fn) as it computes during read, not write */
  function viewWithToggles(value) {
    if (arguments.length === 0) {
      return "undefined" !== typeof view() ? passThruParams(self, view(), [{key: "ui", val: "test"}]) : view();
    }

    return view(value);
  }

  (Object.assign || $.extend)(this, {url, view: viewWithToggles, data, load, fetch, errors});
}

