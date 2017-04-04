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
;(function () {
  "use strict";

  const m      = require("mithril");
  const Stream = require("mithril/stream");

  function FiltersModel(url, errors) {

    const filters = Stream();

    function fetchFilters() {
      m.request({
        method:  "GET",
        url:     url,
        headers: {
          Accept: "application/vnd.go.cd.v4+json"
        }
      }).then(function (data) {
        filters(data);
      });
    }

    function deleteFilter(e) {
      e.preventDefault();
      let button = e.currentTarget;

      let id = parseInt(button.getAttribute("data-filter-id"), 10);

      m.request({
        method:  "DELETE",
        url:     url + "/" + id,
        headers: {
          Accept: "application/vnd.go.cd.v4+json"
        }
      }).then(function (data) {
        filters(data);
      });
    }

    this.load    = fetchFilters;
    this.delete  = deleteFilter;
    this.filters = filters;
    this.errors = errors;
  }

  module.exports = FiltersModel;
})();
