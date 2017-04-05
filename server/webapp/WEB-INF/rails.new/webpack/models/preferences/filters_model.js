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

    // persists checkbox state, defaulting to true. without this Stream,
    // without this the checkbox will be reset each time a filter is created,
    // which is an unexpected user experience.
    const myCommits = Stream(true);

    function fetchFilters() {
      m.request({
        method:  "GET",
        url:     url,
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then(function (data) {
        filters(data);
      });
    }

    function serialize(form) {
      let data = new FormData(form);
      let result = {}, i = data.entries(), current;

      while (!(current = i.next()).done) {
        let entry = current.value;
        result[entry[0]] = entry[1];
      }

      return result;
    }

    function createFilter(e) {
      e.preventDefault();
      errors(null);

      let form = e.currentTarget;

      m.request({
        method:  "POST",
        url:     url,
        data:    serialize(form),
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then(function (data) {
        filters(data);
      }, function fail(data) {
        errors(data.message);
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
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then(function (data) {
        filters(data);
      });
    }

    this.load    = fetchFilters;
    this.save    = createFilter;
    this.delete  = deleteFilter;
    this.myCommits = myCommits;
    this.filters = filters;
    this.errors = errors;
  }

  module.exports = FiltersModel;
})();
