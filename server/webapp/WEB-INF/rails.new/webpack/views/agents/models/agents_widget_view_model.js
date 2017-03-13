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

const Stream = require('mithril/stream');
const _      = require('lodash');

const VM         = () => {
  const dropdownStates     = {};
  const agentCheckedStates = {};
  const allAgentsSelected  = Stream(false);

  const viewModel = {
    dropdown: {
      reset: Stream(true),

      create(dropDownName) {
        if (!dropdownStates[dropDownName]) {
          dropdownStates[dropDownName] = Stream(false);
        }

        return dropdownStates[dropDownName];
      },

      hide(dropDownName) {
        viewModel.dropdown.create(dropDownName)(false);
      },

      hideAllDropDowns() {
        if (this.reset()) {
          for (const item in dropdownStates) {
            dropdownStates[item](false);
          }
        }
        this.reset(true);
      },

      hideOtherDropdowns(dropDownName) {
        for (const item in dropdownStates) {
          if (item !== dropDownName) {
            this.hide(item);
          }
        }
      },

      toggleDropDownState(dropDownName) {
        this.reset(false);
        dropdownStates[dropDownName](!dropdownStates[dropDownName]());
        this.hideOtherDropdowns(dropDownName);
      },

      isDropDownOpen(dropDownName) {
        return this.create(dropDownName)();
      }
    },

    filterText: Stream(''),

    agents: {
      isAnyAgentSelected() {
        return _.some(agentCheckedStates, (boxState) => boxState());
      },

      checkboxFor(uuid) {
        return agentCheckedStates[uuid];
      },

      clearAllCheckboxes() {
        _.each(agentCheckedStates, (boxState) => {
          boxState(false);
        });
      },

      selectedAgentsUuids() {
        return _.compact(_.map(agentCheckedStates, (boxSate, agentId) => {
          if (boxSate()) {
            return agentId;
          }
        }));
      },

      areAllAgentsSelected(allAgents) {
        const filterText = viewModel.filterText();

        const isChecked = allAgents().filterBy(filterText).everyAgent((agent) => {
          const agentsCheckedState = agentCheckedStates[agent.uuid()];
          if (agentsCheckedState) {
            return agentsCheckedState();
          }
        });

        allAgentsSelected(isChecked);
        return isChecked;
      },

      selectAllAgents(allAgents) {
        const isChecked  = allAgentsSelected(!allAgentsSelected());
        const filterText = viewModel.filterText();

        allAgents().filterBy(filterText).eachAgent((agent) => {
          agentCheckedStates[agent.uuid()](isChecked);
        });
      }
    },

    initializeWith(newAgents) {
      const newAgentUUIDs             = newAgents.collectAgentProperty('uuid');
      const agentUUIDsKnownToVM       = _.keysIn(agentCheckedStates);
      const agentUUIDsToRemoveFromVM  = _.difference(agentUUIDsKnownToVM, newAgentUUIDs);
      const newAgentUUIDsNotKnownToVM = _.difference(newAgentUUIDs, agentUUIDsKnownToVM);

      _.each(agentUUIDsToRemoveFromVM, (uuid) => {
        delete agentCheckedStates[uuid];
        delete dropdownStates[uuid];
      });

      _.each(newAgentUUIDsNotKnownToVM, (uuid) => {
        agentCheckedStates[uuid] = Stream();
      });
    }
  };
  return viewModel;
};
module.exports = VM;
