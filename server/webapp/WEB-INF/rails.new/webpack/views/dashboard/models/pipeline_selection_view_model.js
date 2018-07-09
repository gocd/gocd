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
const Stream = require('mithril/stream');

const PipelineSelectionVM = function () {
  const pipelineGroupState = {};

  const viewModel = {
    setGroupSelection: (groupName, isSelected) => {
      pipelineGroupState[groupName](isSelected);
    },

    isGroupExpanded: (groupName) => {
      return pipelineGroupState[groupName]();
    },

    toggleGroupSelection: (groupName) => {
      viewModel.setGroupSelection(groupName, !pipelineGroupState[groupName]());
    },

    initialize: (allPipelines) => {
      _.each(allPipelines, (_pipelinesInGroup, groupName) => {
        pipelineGroupState[groupName] = Stream(false);

        viewModel.setGroupSelection(_.keys(allPipelines)[0], true);
      });
    }
  };

  return viewModel;
};


module.exports = PipelineSelectionVM;
