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
  const f = require("helpers/form_helper");

  function AddNotificationFilterWidget(filterModels, pipelineModel) {
    return {
      oninit() {
        filterModels.load();
      },
      view() {
        return m("form", {class: "create-notification-filter", onsubmit: filterModels.save},
          m(f.select, {
            label:      "Pipeline",
            name:       "pipeline",
            items:    pipelineModel.pipelines,
            onchange:   m.withAttr("value", pipelineModel.currentPipeline)
          }),
          m(f.select, {label: "Stage", name: "stage", items: pipelineModel.stages}),
          m(f.select, {label: "Event", name: "event", items: pipelineModel.events}),
          m(f.checkbox, {label: "Only if it contains my check-ins", name: "myCheckin", checked: filterModels.myCommits(), onchange: m.withAttr("checked", filterModels.myCommits)}),
          m("fieldset",
            m("input", {type: "submit", value: "Add", class: "primary"}),
            m("input", {type: "reset", value: "Reset"})
          )
        );
      }
    };
  }

  module.exports = AddNotificationFilterWidget;
})();

