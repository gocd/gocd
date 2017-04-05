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

  function FiltersModel(apiUrl, errors) {

    const filters = Stream();

    // persists checkbox state, defaulting to true. without this Stream,
    // without this the checkbox will be reset each time a filter is created,
    // which is an unexpected user experience.
    const myCommits = Stream(true);

    function fetchFilters() {
      m.request({
        method:  "GET",
        url:     apiUrl,
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then((data) => {
        filters(data);
      });
    }

    function serialize(form) {
      const data = new FormData(form), result = {}, i = data.entries();
      let current;

      while (!(current = i.next()).done) {
        const entry = current.value;
        result[entry[0]] = entry[1];
      }

      return result;
    }

    function createFilter(e) {
      e.preventDefault();
      errors(null);

      const form = e.currentTarget;

      m.request({
        method:  "POST",
        url:     apiUrl,
        data:    serialize(form),
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then((data) => {
        filters(data);
      }, (data) => {
        errors(data.message);
      });
    }

    function deleteFilter(e) {
      e.preventDefault();

      const button = e.currentTarget;
      const id = parseInt(button.getAttribute("data-filter-id"), 10);

      m.request({
        method:  "DELETE",
        url:     `${apiUrl}/${id}`,
        headers: {
          Accept: "application/vnd.go.cd.v1+json"
        }
      }).then((data) => {
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
