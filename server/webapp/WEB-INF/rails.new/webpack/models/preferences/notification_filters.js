/*
 * Copyright 2017 ThoughtWorks, Inc.
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
;(function () { // eslint-disable-line no-extra-semi
  "use strict";

  const m      = require("mithril");
  const Stream = require("mithril/stream");
  const mr     = require("helpers/mrequest");
  const _      = require("lodash");
  const $      = require("jquery");

  function req(method, url, data) {
    const headers = { "Accept": mr.versionHeader("v1") }, timeout = mr.timeout, contentType = "application/json", dataType = "json";
    return $.ajax(_.omitBy({ method, url, timeout, headers, data, contentType, dataType }, _.isEmpty));
  }

  function serialize(form) {
    const data = new FormData(form), result = {}, iter = data.entries();
    let current;

    while (!(current = iter.next()).done) {
      const entry = current.value;
      result[entry[0]] = entry[1];
    }

    result.match_commits = !!result.myCheckin; // eslint-disable-line camelcase
    delete result.myCheckin;

    return JSON.stringify(result);
  }

  function NotificationFilters(apiUrl, errors) {
    const filters = this.filters = Stream();

    // persists checkbox state, defaulting to true. without this Stream,
    // without this the checkbox will be reset each time a filter is created,
    // which is an unexpected user experience.
    this.myCommits = Stream(true);

    const handleError = ({responseJSON}) => errors(responseJSON.message);
    const redraw      = () => m.redraw();

    function fetchFilters() {
      req("GET", apiUrl).done((data) => filters(data.filters)).fail(handleError).always(redraw);
    }

    function createFilter(e) {
      e.preventDefault();
      errors(null);

      req("POST", apiUrl, serialize(e.currentTarget)).done((data) => filters(data.filters)).fail(handleError).always(redraw);
    }

    function deleteFilter(e) {
      e.preventDefault();
      errors(null);

      const id = parseInt(e.currentTarget.getAttribute("data-filter-id"), 10);
      req("DELETE", `${apiUrl}/${id}`).done((data) => filters(data.filters)).fail(handleError).always(redraw);
    }

    function reset() {
      this.myCommits(true);
    }

    this.load    = fetchFilters;
    this.save    = createFilter;
    this.delete  = deleteFilter;
    this.reset   = reset;
    this.errors  = errors;
  }

  module.exports = NotificationFilters;
})();
