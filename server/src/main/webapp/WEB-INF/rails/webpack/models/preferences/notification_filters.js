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
"use strict";

import m from "mithril";
import Stream from "mithril/stream";
import {mrequest} from "helpers/mrequest";
import _ from "lodash";
import $ from "jquery";

function req(method, url, data) {
  const headers = {"Accept": mrequest.versionHeader()}, timeout = mrequest.timeout, contentType = "application/json",
        dataType                                                                        = "json";
  return $.ajax(_.omitBy({method, url, timeout, headers, data, contentType, dataType}, _.isEmpty));
}

function serialize(form) {
  const formData = $(form).serializeArray();
  const jsonData = {};
  $.each(formData, function () {
    if (jsonData[this.name]) {
      if (!jsonData[this.name].push) {
        jsonData[this.name] = [jsonData[this.name]];
      }
      jsonData[this.name].push(this.value || '');
    } else {
      if (this.name === 'match_commits' && this.value === 'on') {
        if (this.value === 'on') {
          jsonData[this.name] = true;
        } else {
          jsonData[this.name] = false;
        }
      } else {
        jsonData[this.name] = this.value || '';
      }
    }
  });
  return JSON.stringify(jsonData);
}

export function NotificationFilters(apiUrl, errors) {
  const filters = this.filters = Stream();

  // persists checkbox state, defaulting to true. without this Stream,
  // without this the checkbox will be reset each time a filter is created,
  // which is an unexpected user experience.
  this.myCommits = Stream(true);

  const handleError = ({responseJSON}) => errors(responseJSON.message);
  const redraw      = () => m.redraw();

  function fetchFilters() {
    req("GET", apiUrl).done((data) => filters(data._embedded.filters)).fail(handleError).always(redraw);
  }

  function createFilter(e) {
    e.preventDefault();
    errors(null);

    req("POST", apiUrl, serialize(e.currentTarget)).done(fetchFilters).fail(handleError).always(redraw);
  }

  function deleteFilter(e) {
    e.preventDefault();
    errors(null);

    const id = parseInt(e.currentTarget.getAttribute("data-filter-id"), 10);
    req("DELETE", `${apiUrl}/${id}`).done(fetchFilters).fail(handleError).always(redraw);
  }

  function reset() {
    this.myCommits(true);
  }

  this.load   = fetchFilters;
  this.save   = createFilter;
  this.delete = deleteFilter;
  this.reset  = reset;
  this.errors = errors;
}

