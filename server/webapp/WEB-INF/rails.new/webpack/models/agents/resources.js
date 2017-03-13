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

const $                = require('jquery');
const m                = require('mithril');
const _                = require('lodash');
const mrequest         = require('helpers/mrequest');
const TriStateCheckbox = require('models/agents/tri_state_checkbox');
const Routes           = require('gen/js-routes');
const Resources        = {};
Resources.list       = [];

const getSortedResources = (resources, selectedAgents) => {
  const selectedAgentsResources = _.map(selectedAgents, (agent) => agent.resources());

  return _.map(resources.sort(), (resource) => new TriStateCheckbox(resource, selectedAgentsResources));
};

Resources.init = (selectedAgents) => {
  $.ajax({
    method:     'GET',
    url:        Routes.apiv1AdminInternalResourcesPath(),
    beforeSend: mrequest.xhrConfig.forVersion('v1')
  }).then((data) => {
    Resources.list = getSortedResources(data, selectedAgents);
  }).always(m.redraw);
};

module.exports = Resources;
