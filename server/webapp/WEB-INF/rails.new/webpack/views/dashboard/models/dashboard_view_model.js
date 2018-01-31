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

const Stream = require('mithril/stream');
const _      = require('lodash');

const VM = (allPipelines) => {
  const dropdownStates = {};

  const viewModel = {
    dropdown: {
      create: (name) => {
        dropdownStates[name] = Stream(false);
      },

      isDropDownOpen: (name) => dropdownStates[name](),

      toggle: (name) => {
        viewModel.dropdown.hideAllExcept(name);
        dropdownStates[name](!dropdownStates[name]());
      },

      hideAllExcept: (name) => {
        _.each(_.keys(dropdownStates), (key) => (key !== name) && dropdownStates[key](false));
      },

      hideAll: () => {
        _.each(_.keys(dropdownStates), (name) => dropdownStates[name](false));
      },

      //used by tests
      size: () => _.keys(dropdownStates).length
    }
  };

  const initialize = (allPipelines) => {
    _.each(allPipelines, viewModel.dropdown.create);
  };

  initialize(allPipelines);
  return viewModel;
};

module.exports = VM;
