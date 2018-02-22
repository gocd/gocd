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

const m      = require('mithril');
const Stream = require('mithril/stream');
const _      = require('lodash');

const VM = () => {
  const DROPDOWN_KEY                          = 'dropdown';
  const FLASH_MESSAGE_KEY                     = 'flashMessage';
  const FLASH_MESSAGE_TYPE_KEY                = 'flashMessageType';
  const SUCCESS_TYPE                          = "success";
  const FAILURE_TYPE                          = "error";
  const MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS = 5000;

  const pipelinesState       = {};
  const personalizeViewState = Stream(false);

  const viewModel = {
    personalizeView: {
      show: () => {
        personalizeViewState(true);
      },

      hide: () => {
        personalizeViewState(false);
      },

      toggle: () => {
        personalizeViewState(!personalizeViewState());
      },

      isOpen: () => personalizeViewState()
    },

    dropdown: {
      isDropDownOpen: (name, instanceCounter) => pipelinesState[name][DROPDOWN_KEY][instanceCounter](),

      toggle: (name, instanceCounter) => {
        viewModel.dropdown.hideAllExcept(name, instanceCounter);
        pipelinesState[name][DROPDOWN_KEY][instanceCounter](!pipelinesState[name][DROPDOWN_KEY][instanceCounter]());
      },

      hideAllExcept: (name, instanceCounter) => {
        const hideDropDownForOtherInstances = (name, instanceCounter) => {
          _.each(pipelinesState[name][DROPDOWN_KEY], (_instanceCounterState, counter) => {
            if (+(counter) !== instanceCounter) { //counter is a string
              pipelinesState[name][DROPDOWN_KEY][counter](false);
            }
          });
        };

        _.each(pipelinesState, (_pipelineState, pipelineName) => {
          if (pipelineName !== name) {
            viewModel.dropdown.hide(pipelineName);
          } else {
            hideDropDownForOtherInstances(name, instanceCounter);
          }
        });
      },

      hide: (name) => {
        _.each(pipelinesState[name][DROPDOWN_KEY], (instanceCounter) => {
          //put inside curly braces because returning a false will exit the iteration early
          instanceCounter(false);
        });
      },

      hideAll: () => {
        _.each(pipelinesState, (_pipelineState, name) => {
          viewModel.dropdown.hide(name);
        });
      }
    },

    operationMessages: {
      messageFor: (name) => pipelinesState[name][FLASH_MESSAGE_KEY](),

      messageTypeFor: (name) => pipelinesState[name][FLASH_MESSAGE_TYPE_KEY](),

      success: (name, message) => {
        pipelinesState[name][FLASH_MESSAGE_KEY](message);
        pipelinesState[name][FLASH_MESSAGE_TYPE_KEY](SUCCESS_TYPE);
        clearAfterTimeout(name);
      },

      failure: (name, message) => {
        pipelinesState[name][FLASH_MESSAGE_KEY](message);
        pipelinesState[name][FLASH_MESSAGE_TYPE_KEY](FAILURE_TYPE);
        clearAfterTimeout(name);
      }
    },

    //used by tests
    size:     () => _.keys(pipelinesState).length,
    contains: (name) => !_.isEmpty(pipelinesState[name]),

    initialize: (dashboard) => {
      const pipelineNames            = dashboard.allPipelineNames();
      const pipelinesKnownToVM       = _.keysIn(pipelinesState);
      const pipelinesToRemoveFromVM  = _.difference(pipelinesKnownToVM, pipelineNames);
      const newPipelinesNotKnownToVM = _.difference(pipelineNames, pipelinesKnownToVM);

      _.each(pipelinesToRemoveFromVM, (name) => {
        delete pipelinesState[name];
      });

      _.each(newPipelinesNotKnownToVM, (name) => {
        create(name, dashboard.findPipeline(name).getInstanceCounters());
      });

      //pipeline instance changes
      _.each(pipelinesState, (state, pipelineName) => {
        const currentInstances = _.keys(state[DROPDOWN_KEY]);
        const newInstances     = dashboard.findPipeline(pipelineName).getInstanceCounters().map((c) => `${c}`);

        const instancesToRemoveFromVM = _.difference(currentInstances, newInstances);
        const instancesNotKnownToVM   = _.difference(newInstances, currentInstances);

        _.each(instancesToRemoveFromVM, (counter) => {
          delete state[DROPDOWN_KEY][counter];
        });

        _.each(instancesNotKnownToVM, (counter) => {
          state[DROPDOWN_KEY][counter] = Stream(false);
        });
      });
    }
  };

  function clearAfterTimeout(name) {
    setTimeout(() => {
      pipelinesState[name][FLASH_MESSAGE_KEY](undefined);
      pipelinesState[name][FLASH_MESSAGE_TYPE_KEY](undefined);
      m.redraw();
    }, MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS);
  }

  function create(name, instanceCounters) {
    pipelinesState[name]          = {};
    const instanceDropdownTracker = {};
    _.each(instanceCounters, (counter) => instanceDropdownTracker[counter] = Stream(false));

    pipelinesState[name][DROPDOWN_KEY]           = instanceDropdownTracker;
    pipelinesState[name][FLASH_MESSAGE_KEY]      = Stream();
    pipelinesState[name][FLASH_MESSAGE_TYPE_KEY] = Stream();
  }

  return viewModel;
};

module.exports = VM;
