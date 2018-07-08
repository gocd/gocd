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

function boolByName(s, name) {
  return function boundToName(boolPreviousValue) {
    if (!arguments.length) { return s[name](); }
    s[name](!boolPreviousValue);
  };
}

function groupSelection(s, pipelines) {
  const streams = _.map(pipelines, (p) => s[p]);
  const allSelected = Stream.combine(() => _.every(streams, (st) => st()), streams);

  return function(boolPreviousValue) {
    if (!arguments.length) { return allSelected(); }
    _.each(streams, (st) => { st(!boolPreviousValue); });
  };
}

function PipelineListVM(pipelinesByGroup, currentSelection) {
  const displayed = _.reduce(pipelinesByGroup, (memo, pip, grp) => {
    memo[grp] = {
      expanded: Stream(true),
      selected: groupSelection(currentSelection, pip),
      pipelines: _.map(pip, (p) => { return {name: p, selected: boolByName(currentSelection, p)}; })
    };
    return memo;
  }, {});

  this.displayedList = function displayedList() { return displayed; };

  this.selectAll = function selectAll() { _.each(currentSelection, (s, _n) => { s(true); }); };
  this.selectNone = function selectNone() { _.each(currentSelection, (s, _n) => { s(false); }); };
}

module.exports = PipelineListVM;
