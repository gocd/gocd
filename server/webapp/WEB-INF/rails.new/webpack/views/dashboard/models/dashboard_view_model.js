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

  const pipelinesState = {};

  const viewModel = {
    searchText: Stream(''),

    dropdown: {
      isDropDownOpen: (name) => pipelinesState[name][DROPDOWN_KEY](),

      toggle: (name) => {
        viewModel.dropdown.hideAllExcept(name);
        pipelinesState[name][DROPDOWN_KEY](!pipelinesState[name][DROPDOWN_KEY]());
      },

      hideAllExcept: (name) => {
        _.each(_.keys(pipelinesState), (key) => {
          if (key !== name) {
            pipelinesState[key][DROPDOWN_KEY](false);
          }
        });
      },

      hideAll: () => {
        _.each(_.keys(pipelinesState), (name) => {
          pipelinesState[name][DROPDOWN_KEY](false);
        });
      },
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

    initialize: (pipelineNames) => {
      const pipelinesKnownToVM       = _.keysIn(pipelinesState);
      const pipelinesToRemoveFromVM  = _.difference(pipelinesKnownToVM, pipelineNames);
      const newPipelinesNotKnownToVM = _.difference(pipelineNames, pipelinesKnownToVM);

      _.each(pipelinesToRemoveFromVM, (name) => {
        delete pipelinesState[name];
      });

      _.each(newPipelinesNotKnownToVM, create);
    }
  };

  function clearAfterTimeout(name) {
    setTimeout(() => {
      pipelinesState[name][FLASH_MESSAGE_KEY](undefined);
      pipelinesState[name][FLASH_MESSAGE_TYPE_KEY](undefined);
      m.redraw();
    }, MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS);
  }

  function create(name) {
    pipelinesState[name] = {};

    pipelinesState[name][DROPDOWN_KEY]           = Stream(false);
    pipelinesState[name][FLASH_MESSAGE_KEY]      = Stream();
    pipelinesState[name][FLASH_MESSAGE_TYPE_KEY] = Stream();
  }

  return viewModel;
};

module.exports = VM;
