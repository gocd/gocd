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

let m                     = require('mithril');
let f                     = require('helpers/form_helper');
let AgentStateCountWidget = require('views/agents/agent_state_count_widget');
let AgentRowWidget        = require('views/agents/agent_row_widget');
let AgentTableHeader      = require('views/agents/agent_table_header');
let ButtonRowWidget       = require('views/agents/button_row_widget');

const Stream = require('mithril/stream');
const _      = require('lodash');

const AgentsWidget = {
  oninit (vnode) {
    const args = vnode.attrs;

    const clearAllCheckboxes = function () {
      args.vm.agents.clearAllCheckboxes();
    };

    const isUserAdmin = function () {
      return args.isUserAdmin;
    };

    this.message = Stream({});

    this.type = Stream();

    this.allAgents = args.allAgents;

    this.donePerformingUpdate = function () {
      clearAllCheckboxes();
      args.doRefreshImmediately();
    };

    this.hideDropDownsAndMessage = function () {
      args.vm.dropdown.hideAllDropDowns();
    };

    this.selectAllAgents = function () {
      args.vm.agents.selectAllAgents(this.allAgents);
    };

    this.areAllAgentsSelected = function () {
      return args.vm.agents.areAllAgentsSelected(this.allAgents);
    };

    this.selectedAgentsUuids = function () {
      return args.vm.agents.selectedAgentsUuids();
    };

    this.selectedAgentsCount = function () {
      return this.selectedAgentsUuids().length;
    };

    this.displaySuccessMessage = function (action) {
      const count = this.selectedAgentsCount();
      this.message({type: 'success', message: `${action} ${count} ${_('agent').pluralize(count)}`});
    };

    this.displayErrorMessage = function (message) {
      this.message({type: 'warning', message});
    };

    this.displayModifiedMessage = function (action, addedItems, removedItems) {
      const count = this.selectedAgentsCount();
      this.message({
        type:    'success',
        message: `${_(action).pluralize(_.compact(_.concat(addedItems, removedItems)).length)} modified on ${count} ${_('agent').pluralize(count)}`
      });
    };

    this.disableAgents = function () {
      args.doCancelPolling();
      this.allAgents().disableAgents(this.selectedAgentsUuids())
        .then(this.displaySuccessMessage.bind(this, 'Disabled'), this.displayErrorMessage.bind(this))
        .then(this.donePerformingUpdate.bind(this))
        .always(m.redraw);
    };

    this.enableAgents = function () {
      args.doCancelPolling();
      this.allAgents().enableAgents(this.selectedAgentsUuids())
        .then(this.displaySuccessMessage.bind(this, 'Enabled'), this.displayErrorMessage.bind(this))
        .then(this.donePerformingUpdate.bind(this))
        .always(m.redraw);
    };

    this.deleteAgents = function () {
      args.doCancelPolling();
      this.allAgents().deleteAgents(this.selectedAgentsUuids())
        .then(this.displaySuccessMessage.bind(this, 'Deleted'), this.displayErrorMessage.bind(this))
        .then(this.donePerformingUpdate.bind(this))
        .always(m.redraw);
    };

    this.updateResources = function (addResources, removeResources) {
      args.doCancelPolling();
      this.allAgents().updateResources(this.selectedAgentsUuids(), addResources, removeResources)
        .then(this.displayModifiedMessage.bind(this, 'Resource', addResources, removeResources), this.displayErrorMessage.bind(this))
        .then(this.donePerformingUpdate.bind(this))
        .always(m.redraw);
    };

    this.updateEnvironments = function (addEnvironments, removeEnvironments) {
      args.doCancelPolling();
      this.allAgents().updateEnvironments(this.selectedAgentsUuids(), addEnvironments, removeEnvironments)
        .then(this.displayModifiedMessage.bind(this, 'Environment', addEnvironments, removeEnvironments), this.displayErrorMessage.bind(this))
        .then(this.donePerformingUpdate.bind(this))
        .always(m.redraw);
    };

    this.findAgent = function (uuid) {
      return this.allAgents().findAgentByUuid(uuid);
    };

    this.selectedAgents = function () {
      return _.map(this.selectedAgentsUuids(), this.findAgent.bind(this));
    };

    this.filteredAndSortedAgents = function () {
      return this.allAgents().filterBy(args.vm.filterText()).sortBy(args.sortOrder().sortBy(), args.sortOrder().orderBy());
    };

    this.areOperationsAllowed = function () {
      return args.vm.agents.isAnyAgentSelected() && isUserAdmin();
    };
  },

  view (vnode) {
    const ctrl = vnode.state;
    const args = vnode.attrs;

    if (args.showSpinner()) {
      return (<span class="page-spinner"/>);
    }

    const filterText                     = args.vm.filterText();
    const filteredAgents                 = ctrl.filteredAndSortedAgents();
    const maxCharsToBeDisplayedInMessage = 150;
    const classForTableBody              = ["agents-table-body"];

    let updateMessage;
    let permanentMessage;

    if (args.permanentMessage().message) {
      permanentMessage = (
        <f.row class="message">
          <div data-alert class={`callout radius ${  args.permanentMessage().type}`}
               title={args.permanentMessage().message}>

            {_.truncate(args.permanentMessage().message, {'length': maxCharsToBeDisplayedInMessage})}
            <a href="#" class="close"></a>
          </div>
        </f.row>
      );
      classForTableBody.push("has-permanent-message");
    }

    if (ctrl.message().message) {
      updateMessage = (
        <f.row class="message">
          <div data-alert class={`callout radius ${  ctrl.message().type}`} title={ctrl.message().message}>

            {_.truncate(ctrl.message().message, {'length': maxCharsToBeDisplayedInMessage})}
            <a href="#" class="close"/>
          </div>
        </f.row>
      );
      classForTableBody.push("has-message");
    }

    return (
      <div onclick={ctrl.hideDropDownsAndMessage.bind(ctrl)}>
        <div class="header-panel">
          <ButtonRowWidget areOperationsAllowed={ ctrl.areOperationsAllowed.bind(ctrl) }
                           dropdown={args.vm.dropdown}
                           selectedAgents={ ctrl.selectedAgents.bind(ctrl) }
                           onDisable={ ctrl.disableAgents.bind(ctrl) }
                           onEnable={ ctrl.enableAgents.bind(ctrl) }
                           onDelete={ ctrl.deleteAgents.bind(ctrl) }
                           onResourcesUpdate={ ctrl.updateResources.bind(ctrl) }
                           onEnvironmentsUpdate={ ctrl.updateEnvironments.bind(ctrl) }/>

          <div class="search-panel">
            <f.row>
              <f.column size={6} largeSize={3}>
                <div class="search-bar">
                  <input type="text"
                         oninput={m.withAttr("value", args.vm.filterText)}
                         value={filterText}
                         placeholder="Filter Agents"
                         id="filter-agent"
                         class="filter-agent"
                  />
                </div>
              </f.column>

              <f.column size={6} largeSize={8}>
                <AgentStateCountWidget agents={ctrl.filteredAndSortedAgents.bind(ctrl)}/>
              </f.column>

            </f.row>
          </div>
          {permanentMessage}
          {updateMessage}
          <f.row>
            <div class="container">
              <table class="go-table agents-table">
                <AgentTableHeader
                  sortOrder={args.sortOrder}
                  onCheckboxClick={ctrl.selectAllAgents.bind(ctrl)}
                  checkboxValue={ctrl.areAllAgentsSelected.bind(ctrl)}
                  isUserAdmin={args.isUserAdmin}/>
              </table>
            </div>
          </f.row>
        </div>
        <f.row class={classForTableBody}>
          <div class="container">
            <table class="go-table agents-table">
              <tbody>
              {
                filteredAgents.mapAgents((agent) => {
                  const uuid          = agent.uuid();
                  const checkboxModel = args.vm.agents.checkboxFor(uuid);

                  return (
                    <AgentRowWidget agent={agent}
                                    key={uuid}
                                    checkBoxModel={checkboxModel}
                                    show={agent.matches(filterText)}
                                    dropdown={args.vm.dropdown}
                                    isUserAdmin={args.isUserAdmin}

                    />
                  );

                })
              }
              </tbody>
            </table>
          </div>
        </f.row>
      </div>
    );
  }
};

module.exports = AgentsWidget;
