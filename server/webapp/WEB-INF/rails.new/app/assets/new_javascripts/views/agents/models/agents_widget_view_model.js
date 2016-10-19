/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash'], function (m, _) {

  var VM = function () {
    var dropdownStates     = {};
    var agentCheckedStates = {};
    var allAgentsSelected  = m.prop(false);

    var viewModel = {
      dropdown: {
        reset: m.prop(true),

        create: function (dropDownName) {
          if (!dropdownStates[dropDownName]) {
            dropdownStates[dropDownName] = m.prop(false);
          }

          return dropdownStates[dropDownName];
        },

        hide: function (dropDownName) {
          viewModel.dropdown.create(dropDownName)(false);
        },

        hideAllDropDowns: function () {
          if (this.reset()) {
            for (var item in dropdownStates) {
              dropdownStates[item](false);
            }
          }
          this.reset(true);
        },

        hideOtherDropdowns: function (dropDownName) {
          for (var item in dropdownStates) {
            if (item !== dropDownName) {
              this.hide(item);
            }
          }
        },

        toggleDropDownState: function (dropDownName) {
          this.reset(false);
          dropdownStates[dropDownName](!dropdownStates[dropDownName]());
          this.hideOtherDropdowns(dropDownName);
        },

        isDropDownOpen: function (dropDownName) {
          return this.create(dropDownName)();
        }
      },

      filterText: m.prop(''),

      agents: {
        isAnyAgentSelected: function () {
          return _.some(agentCheckedStates, function (boxState) {
            return boxState();
          });
        },

        checkboxFor: function (uuid) {
          return agentCheckedStates[uuid];
        },

        clearAllCheckboxes: function () {
          _.each(agentCheckedStates, function (boxState) {
            boxState(false);
          });
        },

        selectedAgentsUuids: function () {
          return _.compact(_.map(agentCheckedStates, function (boxSate, agentId) {
            if (boxSate()) {
              return agentId;
            }
          }));
        },

        areAllAgentsSelected: function (allAgents) {
          var filterText = viewModel.filterText();

          var isChecked = allAgents().filterBy(filterText).everyAgent(function (agent) {
            var agentsCheckedState = agentCheckedStates[agent.uuid()];
            if (agentsCheckedState) {
              return agentsCheckedState();
            }
          });

          allAgentsSelected(isChecked);
          return isChecked;
        },

        selectAllAgents: function (allAgents) {
          var isChecked  = allAgentsSelected(!allAgentsSelected());
          var filterText = viewModel.filterText();

          allAgents().filterBy(filterText).eachAgent(function (agent) {
            agentCheckedStates[agent.uuid()](isChecked);
          });
        }
      },

      initializeWith: function (newAgents) {
        var newAgentUUIDs             = newAgents.collectAgentProperty('uuid');
        var agentUUIDsKnownToVM       = _.keysIn(agentCheckedStates);
        var agentUUIDsToRemoveFromVM  = _.difference(agentUUIDsKnownToVM, newAgentUUIDs);
        var newAgentUUIDsNotKnownToVM = _.difference(newAgentUUIDs, agentUUIDsKnownToVM);

        _.each(agentUUIDsToRemoveFromVM, function (uuid) {
          delete agentCheckedStates[uuid];
          delete dropdownStates[uuid];
        });

        _.each(newAgentUUIDsNotKnownToVM, function (uuid) {
          agentCheckedStates[uuid] = m.prop();
        });
      }
    };
    return viewModel;
  };
  return VM;
});
