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

function DashboardViewModel() {
  const MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS = 5000;

  const pipelineFlashMessages = {};
  let dropdownPipelineName, dropdownPipelineCounter;

  const viewModel = {
    dropdown: {
      isOpen: (name, instanceCounter) => ((name === dropdownPipelineName) && (instanceCounter === dropdownPipelineCounter)),

      show: (name, instanceCounter) => {
        dropdownPipelineName    = name;
        dropdownPipelineCounter = instanceCounter;
      },

      hide: () => {
        dropdownPipelineName    = undefined;
        dropdownPipelineCounter = undefined;
      }
    },

    buildCause: Stream(),

    operationMessages: {
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
    }
  };

  function clearAfterTimeout(name) {
    setTimeout(() => {
      delete pipelineFlashMessages[name];
      m.redraw();
    }, MESSAGE_CLEAR_TIMEOUT_IN_MILLISECONDS);
  }

  return viewModel;
}

module.exports = DashboardViewModel;
