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

import $ from "jquery";
import Stream from "mithril/stream";

export class Frame {
  title: Stream<string>               = Stream();
  url: Stream<string>                 = Stream();
  view: Stream<string>                = Stream();
  pluginId: Stream<string>            = Stream();
  data: Stream<object>                = Stream();
  errors: Stream<JQuery.jqXHR | null> = Stream();
  readonly uid: string;
  private nonce                       = 0;

  constructor(uid: string) {
    this.uid = uid;
  }

  load(before: () => void, after: () => void) {
    this.errors(null);
    $.ajax({
             url: this.url(),
             type: "GET",
             dataType: "json",
             beforeSend: () => before && before()
           })
     .done((r) => {
       this.data(r.data);
       this.view(this.withParam(r.view_path, "~n.o.n.c.e", this.nonce++));
     })
     .fail((xhr) => {
       this.errors(xhr);
     }).always(() => after && after());
  }

  fetch(url: string, handler: (data: object | null, errors: JQuery.jqXHR | null) => void) {
    this.errors(null);
    $.ajax({
             url,
             type: "GET",
             dataType: "json"
           })
     .done((r) => {
       handler(r.data, null);
     }).fail((xhr: JQuery.jqXHR) => {
      this.errors(xhr);
      handler(null, this.errors());
    });
  }

  withParam(url: string, key: string, value: string | number) {
    if (!key) {
      return url;
    }

    const enc = encodeURIComponent;
    const p   = `${value}` ? `${enc(key)}=${enc(value)}` : enc(key);
    return url + (url.match(/[&?]/) ? "&" : "?") + p;
  }
}
