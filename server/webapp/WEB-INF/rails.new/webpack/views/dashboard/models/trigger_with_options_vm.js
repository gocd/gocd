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

const TriggerWitHOptionsVM = function () {
  const materialsState = {};
  const tabsState      = {};

  const viewModel = {
    MATERIALS_TAB_KEY:                    'materials',
    ENVIRONMENT_VARIABLES_TAB_KEY:        'environment-variables',
    SECURE_ENVIRONMENT_VARIABLES_TAB_KEY: 'secure-environment-variables',

    isTabSelected: (tabKey) => tabsState[tabKey](),

    selectTab: (tabKey) => {
      _.each(_.keys(tabsState), (tab) => {
        (tab === tabKey) ? tabsState[tab](true) : tabsState[tab](false);
      });
    },

    initialize: (triggerWithOptionsInfo) => {
      tabsState[viewModel.MATERIALS_TAB_KEY]                    = Stream(true);
      tabsState[viewModel.ENVIRONMENT_VARIABLES_TAB_KEY]        = Stream(false);
      tabsState[viewModel.SECURE_ENVIRONMENT_VARIABLES_TAB_KEY] = Stream(false);

      _.each(triggerWithOptionsInfo.materials, (material) => {
        materialsState[material] = Stream(false);
      });

      materialsState[triggerWithOptionsInfo.materials[0]](true);
    }
  };


  return viewModel;
};


module.exports = TriggerWitHOptionsVM;
