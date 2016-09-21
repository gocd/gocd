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

    this.dropdown = {
      states: {},

      reset: m.prop(true),

      add: function (name) {
        if (!this.states[name]) {
          this.states[name] = m.prop(false);
        }
      },

      hide: function (name) {
        this.states[name](false);
      },

      hideAllDropDowns: function () {
        if (this.reset()) {
          for (var item in this.states) {
            this.states[item](false);
          }
        }
        this.reset(true);
      },

      hideOtherDropdowns: function (name) {
        for (var item in this.states) {
          if (!_.isEqual(item, name)) {
            this.states[item](false);
          }
        }
      }
    };

    this.agents = {
      all: {
        selected: m.prop(false)
      }
    };

    this.filterText = m.prop('');

    this.agentsCheckedState = {};
  };
  return VM;
});
