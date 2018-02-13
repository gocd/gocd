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

const _      = require('lodash');
const m      = require('mithril');
const Stream = require('mithril/stream');

const sparkRoutes = require('helpers/spark_routes');
const AjaxHelper  = require('helpers/ajax_helper');

const SearchVM = function (pipelineName, materials) {
  const searchState = {};

  _.each(materials, (material) => {
    searchState[material.name] = {
      searchInProgress:      Stream(false),
      searchText:            Stream(''),
      materialSearchResults: Stream([]),
      selectRevision:        (revision) => {
        material.selection(revision);
        searchState[material.name].updateSearchText(revision);
      },
      updateSearchText(newText) {
        searchState[material.name].searchText(newText);
        searchState[material.name].debouncedSearch();
      },
      debouncedSearch:       _.debounce(() => {
        searchState[material.name].performSearch();
      }, 200),

      performSearch() {
        const vm = searchState[material.name];
        this.searchInProgress(true);
        AjaxHelper.GET({
          url:        sparkRoutes.pipelineMaterialSearchPath(pipelineName, material.fingerprint, this.searchText()),
          apiVersion: 'v1',
        }).then(this.materialSearchResults)
          .always(() => {
            vm.searchInProgress(false);
            m.redraw();
          });
      }
    };
  });

  return searchState;
};

module.exports = SearchVM;
