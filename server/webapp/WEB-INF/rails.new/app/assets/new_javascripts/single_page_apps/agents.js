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

require([
  'jquery', 'mithril',
  'models/agents/agents',
  'views/agents/agents_widget',
  'views/agents/models/agents_widget_view_model',
  'foundation.util.mediaQuery', 'foundation.dropdownMenu', 'foundation.responsiveToggle', 'foundation.dropdown'
], function ($, m, Agents, AgentsWidget, AgentsVM) {

  $(function () {

    var agentsDOMElement = document.getElementById('agents');

    var isUserAdmin = JSON.parse($(agentsDOMElement).attr('is-current-user-an-admin'));

    $(document).foundation();

    m.route.mode = "hash";

    var agents = m.prop(new Agents());

    var agentsViewModel = new AgentsVM();
    m.route(agentsDOMElement, '', {
      '':                  m.component(AgentsWidget, {vm: agentsViewModel, allAgents: agents, isUserAdmin: isUserAdmin}),
      '/:sortBy/:orderBy': m.component(AgentsWidget, {vm: agentsViewModel, allAgents: agents, isUserAdmin: isUserAdmin})
    });
  });
});

