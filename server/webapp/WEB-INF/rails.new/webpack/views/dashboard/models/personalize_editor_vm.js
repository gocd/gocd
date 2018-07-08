/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const Stream = require("mithril/stream");
const _      = require("lodash");

const PipelineListVM = require("views/dashboard/models/pipeline_list_vm");
const DashboardFilter = require("models/dashboard/dashboard_filter");

function PersonalizeEditorVM(config, pipelinesByGroup) { // config is usually the current filter
  normalize(config);
  // const existingName = config.name;

  const selectionMap = deriveSelectionMap(pipelinesByGroup, config);

  const name = Stream(config.name);
  const type = Stream(config.type);
  const state = Stream(config.state);
  const pipelines = pipelineMapToList(selectionMap, type);

  this.name = name;
  this.selectionModel = new PipelineListVM(pipelinesByGroup, selectionMap);

  boolToList(this, state, "building");
  boolToList(this, state, "failing");

  this.includeNewPipelines = function (boolPreviousValue) {
    if (!arguments.length) { return "blacklist" === type(); }
    type(!boolPreviousValue ? "blacklist" : "whitelist");
  };

  this.asFilter = () => {
    return new DashboardFilter({name: name(), type: type(), pipelines: pipelines(), state: state()});
  };
}

function normalize(config) {
  config.type = config.type || "blacklist";
  config.pipelines = arr(config.pipelines);
  config.state = arr(config.state);
}

function pipelineMapToList(selectionMap, type) {
  return function pipelines() {
    const invert = "blacklist" === type();
    return _.reduce(selectionMap, (m, v, k) => {
      if (invert ^ v()) { m.push(k); }
      return m;
    }, []);
  };
}

function deriveSelectionMap(pipelinesByGroup, filter) {
  return new DashboardFilter(filter).deriveSelectionMap(pipelinesByGroup);
}

function arr(val) { // ensure value is an array
  if (!val) { return []; }
  if (val instanceof Array) { return val; }
  return [val];
}

// allows a boolean to control the presence of a value in
// a list within a m.Stream
function boolToList(model, stream, attr) {
  model[attr] = function(boolPreviousValue) {
    const r = stream();
    if (!arguments.length) { return _.includes(r, attr); }

    if (!boolPreviousValue) {
      r.push(attr); // toggle to `true` => add to list
    } else {
      r.splice(r.indexOf(attr), 1); // toggle to `false` => remove from list
    }
    stream(r.sort());
  };
}

module.exports = PersonalizeEditorVM;
