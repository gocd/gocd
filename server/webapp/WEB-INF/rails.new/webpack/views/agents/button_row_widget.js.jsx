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
let ResourcesListWidget    = require('views/agents/resources_list_widget');
let EnvironmentsListWidget = require('views/agents/environments_list_widget');

const Stream       = require('mithril/stream');
const Resources    = require('models/agents/resources');
const Environments = require('models/agents/environments');

const ButtonRowWidget = {
  oninit (vnode) {
    const args        = vnode.attrs;
    const self        = this;
    this.resources    = Stream();
    this.environments = Stream();

    this.dropdownClass = function (name) {
      return args.dropdown.isDropDownOpen(name) ? 'has-dropdown is-open' : 'has-dropdown';
    };

    this.resourcesButtonClicked = function (e) {
      e.preventDefault();
      Resources.all(args.selectedAgents())
        .then(self.resources)
        .always(m.redraw);
      args.dropdown.toggleDropDownState('resource');
    };

    this.environmentsButtonClicked = function (e) {
      e.preventDefault();
      Environments.all(args.selectedAgents())
        .then(self.environments)
        .always(m.redraw);
      args.dropdown.toggleDropDownState('environment');
    };

  },

  view (vnode) {
    const ctrl = vnode.state;
    const args = vnode.attrs;

    return (
      <header class="page-header">
        <f.row>
          <f.column size={5}>
            <h1>Agents</h1>
          </f.column>

          <f.column size={7}>
            <ul class="button-group header-panel-button-group">
              <li>
                <f.button onclick={args.onDelete}
                          disabled={!args.areOperationsAllowed()}>
                  Delete
                </f.button>
              </li>

              <li>
                <f.button onclick={args.onDisable}
                          disabled={!args.areOperationsAllowed()}>
                  Disable
                </f.button>
              </li>

              <li>
                <f.button onclick={args.onEnable}
                          disabled={!args.areOperationsAllowed()}>
                  Enable
                </f.button>
              </li>

              <li class={ ctrl.dropdownClass('resource') }>
                <f.button onclick={ctrl.resourcesButtonClicked}
                          disabled={!args.areOperationsAllowed()}>
                  Resources
                </f.button>


                <ResourcesListWidget hideDropDown={args.dropdown.hide}
                                     dropDownReset={args.dropdown.reset}
                                     resources={ctrl.resources}
                                     onResourcesUpdate={args.onResourcesUpdate}/>
              </li>

              <li class={ctrl.dropdownClass('environment')}>
                <f.button onclick={ctrl.environmentsButtonClicked}
                          disabled={!args.areOperationsAllowed()}>
                  Environments
                </f.button>

                <EnvironmentsListWidget hideDropDown={args.dropdown.hide}
                                        dropDownReset={args.dropdown.reset}
                                        environments={ctrl.environments}
                                        onEnvironmentsUpdate={args.onEnvironmentsUpdate}/>
              </li>
            </ul>
          </f.column>
        </f.row>
      </header>
    );
  }
};
module.exports        = ButtonRowWidget;
