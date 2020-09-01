/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import m from "mithril";
import _ from "lodash";
import Stream from "mithril/stream";
import state from "./stage_overview_state";

function FilterMixin() {
  const self = this;
  const internalSearchText = Stream("");

  const performSearch = _.debounce(() => {
    m.route.set(`/${internalSearchText()}`);
    m.redraw();
  }, 200);

  this._performRouting = performSearch;

  this.filteredGroups = (filter) => {
    return this.selectedGroups().select((pipelineName) => {
      return _.includes(pipelineName.toLowerCase(), internalSearchText()) && filter.acceptsStatusOf(self.dashboard.findPipeline(pipelineName));
    }).groups;
  };

  this.searchText = function searchText(searchedBy) {
    if (arguments.length) {
      internalSearchText(searchedBy.toLowerCase());
      self._performRouting();
    } else {
      return internalSearchText();
    }
  };
}

function OperationMessagingMixin() {
  const MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS = 5000;
  const pipelineFlashMessages = {};

  function clearAfterTimeout(name) {
    setTimeout(() => {
      delete pipelineFlashMessages[name];
      m.redraw();
    }, MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS);
  }

  this.operationMessages = {
    get:     (name) => pipelineFlashMessages[name],
    success: (name, message) => {
      pipelineFlashMessages[name] = {
        message,
        type: "success"
      };
      clearAfterTimeout(name);
    },

    failure: (name, message) => {
      pipelineFlashMessages[name] = {
        message,
        type: "error"
      };
      clearAfterTimeout(name);
    }
  };
}

function GroupingMixin() {
  const scheme = Stream("pipeline_groups");

  _.assign(this, {
    scheme,

    groupByItems: [
      {
        id:   "pipeline_groups",
        text: "Pipeline Groups"
      },
      {
        id:   "environments",
        text: "Environments"
      }
    ],

    selectedGroups: () => this.groupByPipelineGroup() ? this.dashboard.getPipelineGroups() : this.dashboard.getEnvironments(),

    groupByPipelineGroup: function groupByPipelineGroup(bool) {
      if (!arguments.length) { return "pipeline_groups" === scheme(); }
      if (bool) { scheme("pipeline_groups"); }
    },

    groupByEnvironment: function groupByEnvironment(bool) {
      if (!arguments.length) { return "environments" === scheme(); }
      if (bool) { scheme("environments"); }
    }
  });
}

export function DashboardViewModel(dashboard) {
  OperationMessagingMixin.call(this);
  GroupingMixin.call(this);
  FilterMixin.call(this);

  let dropdownPipelineName, dropdownPipelineCounter;

  const self = this;

  _.assign(this, {
    dashboard,

    etag: Stream(null),

    invalidateEtag: () => self.etag(null),

    dropdown: {
      isOpen: (name, instanceCounter) => ((name === dropdownPipelineName) && (instanceCounter === dropdownPipelineCounter)),

      show: (name, instanceCounter) => {
        dropdownPipelineName = name;
        dropdownPipelineCounter = instanceCounter;
      },

      hide: () => {
        dropdownPipelineName = undefined;
        dropdownPipelineCounter = undefined;
      }
    },

    stageOverview: state.StageOverviewState,

    buildCause: Stream()
  });
}
