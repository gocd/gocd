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
import _ from "lodash";
import Stream from "mithril/stream";
import _s from "underscore.string";

export function PipelineListVM(pipelinesByGroup, currentSelection) {

  const searchTerm = Stream("");

  const hasSearch = () => !!searchTerm().trim();

  this.searchTerm = searchTerm;

  const fullPipelineList = _.reduce(pipelinesByGroup, (memo, pipelines, group) => {
    const exp = Stream(false);
    const expWithSearch = Stream(true);

    /**
     * This function lets us wrap 2 separate states per group; one while searching, the other while not.
     * By keeping two distinct states, we can restore the user's prior expanded/collapsed states once
     * the user clears the search term.
     */
    function expanded(bool) {
      const search = !_.isEmpty(searchTerm().trim()); // is there a search in progress?

      if (!arguments.length) { return search ? expWithSearch() : (expWithSearch(true) /* reset default state when search is cleared */ && exp()); }

      // depending on whether we are searching, decide which state to persist
      if (!search) { return exp(bool); }
      return expWithSearch(bool);
    }

    memo[group] = {
      expanded,
      selected: groupSelection(currentSelection, pipelines),
      disabled: hasSearch,
      indeterminate: indeterminate(currentSelection, pipelines),
      pipelines: _.map(pipelines, (p) => { return {name: p, selected: boolByName(currentSelection, p)}; })
    };
    return memo;
  }, {});

  this.displayedList = function displayedList() {
    const search = searchTerm().trim().toLowerCase();

    if (!search) { return fullPipelineList; }

    return _.reduce(fullPipelineList, (memo, groupModel, groupName) => {
      const pipelines = _.filter(groupModel.pipelines, (p) => _s.include(p.name.toLowerCase(), search));

      if (pipelines.length) {
        memo[groupName] = _.assign({}, groupModel, { pipelines });
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

  function setAllSelectionsTo(bool) {
    const search = searchTerm().trim().toLowerCase();
    _.each(currentSelection, (s, n) => { _s.include(n.toLowerCase(), search) && s(bool); });
  }

  this.selectAll = () => setAllSelectionsTo(true);
  this.selectNone = () => setAllSelectionsTo(false);

  this.hasSearch = hasSearch;
  this.hasAnySelections = () => !!_.find(currentSelection, (s, _n) => s());
  this.hasAllSelected = () => _.every(currentSelection, (s) => s());
  this.hasNoneSelected = () => _.every(currentSelection, (s) => !s());
}

function boolByName(s, name) {
  return function boundToName(value) {
    if (!arguments.length) { return s[name](); }
    s[name](value);
  };
}

function indeterminate(s, pipelines) {
  return () => {
    const selected = _.filter(pipelines, (p) => s[p]());
    return 0 !== selected.length && selected.length < pipelines.length;
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

