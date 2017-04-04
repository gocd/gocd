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

  const m = require("mithril");
  const _ = require("lodash");

  function FiltersList(model) {
    return {
      oninit(vnode) {
        model.load();
      },
      view(vnode) {
        return m("table", {class: "notification-filters-list"},
          m("thead",
            m("tr",
              m("th", "Pipeline"),
              m("th", "Stage"),
              m("th", "Event"),
              m("th", "Check-ins Matcher"),
              m("th")
            )
          ),
          m("tbody",
            _.map(model.filters(), (filter) => {
              return m("tr",
                m("td", filter.pipelineName),
                m("td", filter.stageName),
                m("td", filter.event),
                m("td", filter.myCheckin ? "Mine" : "All"),
                m("td",
                  m("button", {"data-filter-id": filter.id, onclick: model.delete}, "Delete")
                )
              );
            })
          )
        );
      }
    };
  }

  module.exports = FiltersList;
})();

