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

let m                      = require('mithril');
let f                      = require('helpers/form_helper');
let TriStateCheckboxWidget = require('views/agents/tri_state_checkbox_widget');

const _ = require('lodash');

const EnvironmentsListWidget = {
  oninit (vnode) {
    const args              = vnode.attrs;
    this.updateEnvironments = function (callback) {
      function hideEnvironmentsDropDown() {
        args.hideDropDown('environment');
      }

      hideEnvironmentsDropDown();

      const environmentsToBeAdded = _.filter(args.environments(), (environment) => environment.isChecked()).map((environment) => {
        return environment.name();
      });

      const environmentsToBeRemoved = _.filter(args.environments(), (environment) => !environment.isIndeterminate() && !environment.isChecked()).map((environment) => {
        return environment.name();
      });

      callback(environmentsToBeAdded, environmentsToBeRemoved);
    };

    this.closeDropdown = function () {
      args.dropDownReset(false);
    };
  },

  view (vnode) {

    const ctrl = vnode.state;
    const args = vnode.attrs;

    const isEnvironmentsEmpty    = _.isEmpty(args.environments());
    let environmentDropDownClass = "agent-button-group-dropdown env-dropdown ";

    if (isEnvironmentsEmpty) {
      environmentDropDownClass = `${environmentDropDownClass  }empty`;
    }

    return (
      <div class={environmentDropDownClass} onclick={ctrl.closeDropdown}>
        <ul class="resources-items">
          {_.map(args.environments(), (environment, index) => {
            return (
              <TriStateCheckboxWidget triStateCheckbox={environment} index={index} key={environment.name()}/>);
          })
          }
        </ul>

        {isEnvironmentsEmpty ? (<span class="no-environment">No environments are defined</span>) :
          <f.button
            onclick={ctrl.updateEnvironments.bind(ctrl, args.onEnvironmentsUpdate)}
            class="btn-apply"
            data-toggle="environments-list">
            Apply
          </f.button>
        }
      </div>
    );
  }
};

module.exports = EnvironmentsListWidget;
