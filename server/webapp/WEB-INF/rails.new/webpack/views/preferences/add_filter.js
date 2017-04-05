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

  const m = require("mithril");
  const _ = require("lodash");

  function AddFilter(filterModels, pipelineModel) {
    return {
      oninit() {
        filterModels.load();
      },
      view() {
        return m("form", {class: "create-notification-filter", onsubmit: filterModels.save},
          m(Dropdown, {
            label:      "Pipeline",
            name:       "pipeline",
            options:    pipelineModel.pipelines,
            onchange:   m.withAttr("value", pipelineModel.currentPipeline)
          }),
          m(Dropdown, {label: "Stage", name: "stage", defaultOpt: "[Any Stage]", options: pipelineModel.stages}),
          m(Dropdown, {label: "Event", name: "event", defaultOpt: "All", options: pipelineModel.events}),
          m("label", m("input", {type: "checkbox", name: "myCheckin", checked: filterModels.myCommits(), onchange: m.withAttr("checked", filterModels.myCommits)}), m("span", "Only if it contains my check-ins")),
          m("fieldset",
            m("input", {type: "submit", value: "Add", class: "primary"}),
            m("input", {type: "reset", value: "Reset"})
          )
        );
      }
    };
  }

  const Dropdown = {
    oninit(vnode) {
      _.assign(vnode.state, vnode.attrs);
    },

    view() {
      return m("label",
        m("span", this.label),
        m("select", {name: this.name, onchange: this.onchange},
          _.map(this.options(), (option) => m("option", {value: option}, option))
        )
      );
    }
  };

  module.exports = AddFilter;
})();

