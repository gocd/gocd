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

const $                = require('jquery');
const m                = require('mithril');
const _                = require('lodash');
const mrequest         = require('helpers/mrequest');
const TriStateCheckbox = require('models/agents/tri_state_checkbox');
const Routes           = require('gen/js-routes');
const Environments     = {};
Environments.list    = [];

const getSortedEnvironments = (environments, selectedAgents) => {
  const selectedAgentsEnvironments = _.map(selectedAgents, agent => agent.environments());

  return _.map(environments.sort(), environment => new TriStateCheckbox(environment, selectedAgentsEnvironments));
};

Environments.init = selectedAgents => {
  $.ajax({
    method:     'GET',
    url:        Routes.apiv1AdminInternalEnvironmentsPath(),
    beforeSend: mrequest.xhrConfig.forVersion('v1')
  }).then(data => {
    Environments.list = getSortedEnvironments(data, selectedAgents);
  }).always(m.redraw);
};

module.exports = Environments;
