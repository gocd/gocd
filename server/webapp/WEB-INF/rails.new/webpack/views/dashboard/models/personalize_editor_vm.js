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

const _      = require("lodash");
const Stream = require("mithril/stream");

const PipelineListVM = require("views/dashboard/models/pipeline_list_vm");

function PersonalizeEditorVM(config, pipelinesByGroup) { // config is usually the current filter
  normalize(config);

  const name = Stream(config.name);
  const type = Stream(config.type);
  const state = Stream(config.state);

  const inverted = () => "blacklist" === type();

  this.name = name;
  this.selectionVM = PipelineListVM.create(pipelinesByGroup, inverted(), config.pipelines);

  boolToList(this, state, "building");
  boolToList(this, state, "failing");

  this.includeNewPipelines = function (boolPreviousValue) {
    if (!arguments.length) { return inverted(); }
    type(!boolPreviousValue ? "blacklist" : "whitelist");
  };

  this.asFilter = () => {
    const pipelines = this.selectionVM.pipelines(inverted());
    return {name: name(), type: type(), pipelines, state: state()};
  };
}

function normalize(config) {
  config.type = config.type || "blacklist";
  config.pipelines = arr(config.pipelines);
  config.state = arr(config.state);
}

function arr(val) { // ensure value is an array
  if ("undefined" === typeof val) { return []; }
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
