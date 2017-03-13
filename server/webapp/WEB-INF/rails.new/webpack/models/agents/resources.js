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

var $                = require('jquery');
var m                = require('mithril');
var _                = require('lodash');
var mrequest         = require('helpers/mrequest');
var TriStateCheckbox = require('models/agents/tri_state_checkbox');
var Routes           = require('gen/js-routes');
var Resources        = {};
Resources.list       = [];

var getSortedResources = (resources, selectedAgents) => {
  var selectedAgentsResources = _.map(selectedAgents, agent => agent.resources());

  return _.map(resources.sort(), resource => new TriStateCheckbox(resource, selectedAgentsResources));
};

Resources.init = selectedAgents => {
  $.ajax({
    method:     'GET',
    url:        Routes.apiv1AdminInternalResourcesPath(),
    beforeSend: mrequest.xhrConfig.forVersion('v1')
  }).then(data => {
    Resources.list = getSortedResources(data, selectedAgents);
  }).always(m.redraw);
};

module.exports = Resources;
