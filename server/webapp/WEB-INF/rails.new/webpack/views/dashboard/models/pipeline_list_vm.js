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

function PipelineListVM(pipelinesByGroup, currentSelection) {

  const searchTerm = Stream();

  this.searchTerm = searchTerm;
  this.displayedList = function displayedList() {
    const search = searchTerm();

    return _.reduce(pipelinesByGroup, (memo, pips, grp) => {
      const pipelines = search ? _.filter(pips, (p) => _.includes(p, search)) : pips;

      if (pipelines.length) {
        memo[grp] = {
          expanded: Stream(!!search),
          selected: groupSelection(currentSelection, pips),
          pipelines: _.map(pipelines, (p) => { return {name: p, selected: boolByName(currentSelection, p)}; })
        };
      }
      return memo;
    }, {});
  };

  this.pipelines = function pipelines(invert) {
    return _.reduce(currentSelection, (m, v, k) => {
      if (invert ^ v()) { m.push(k); }
      return m;
    }, []);
  };

  this.selectAll = function selectAll() { _.each(currentSelection, (s, _n) => { s(true); }); };
  this.selectNone = function selectNone() { _.each(currentSelection, (s, _n) => { s(false); }); };
}

function boolByName(s, name) {
  return function boundToName(value) {
    if (!arguments.length) { return s[name](); }
    s[name](value);
  };
}

function groupSelection(s, pipelines) {
  const streams = _.map(pipelines, (p) => s[p]);
  const allSelected = Stream.combine(() => _.every(streams, (st) => st()), streams);

  return function(value) {
    if (!arguments.length) { return allSelected(); }
    _.each(streams, (st) => { st(value); });
  };
}

function calcSelectionMap(pipelinesByGroup, invert, pipelines) {
  return _.reduce(pipelinesByGroup, (m, pip, _n) => {
    _.each(pip, (p) => { m[p] = Stream(!!(invert ^ _.includes(pipelines, p))); });
    return m;
  }, {});
}

_.assign(PipelineListVM, {
  calcSelectionMap,
  create(pipelinesByGroup, invert, pipelines) {
    const selections = calcSelectionMap(pipelinesByGroup, invert, pipelines);
    return new PipelineListVM(pipelinesByGroup, selections);
  }
});

module.exports = PipelineListVM;
