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

const $          = require('jquery');
const Stream     = require('mithril/stream');
const _          = require('lodash');
const s          = require('string-plus');
const Mixins     = require('models/mixins/model_mixins');
const filesize   = require('filesize');
const mrequest   = require('helpers/mrequest');
const Routes     = require('gen/js-routes');
const CrudMixins = require('models/mixins/crud_mixins');

require('lodash-inflection');

const statusComparator = (agent) => {
  const rank = {
    "Pending":              1,
    "LostContact":          2,
    "Missing":              3,
    "Building":             4,
    "Building (Cancelled)": 5,
    "Idle":                 6,
    "Disabled (Building)":  7,
    "Disabled (Cancelled)": 8,
    "Disabled":             9
  };
  return rank[agent.status()];
};

const freeSpaceComparator = (agent) => {
  return parseInt(agent.freeSpace());
};

const sortByAttrName = (attrName) => (agent) => _.toLower(agent[attrName]());

const resolve = (deferred) => (data, _textStatus, jqXHR) => {
  deferred.resolve(mrequest.unwrapMessage(data, jqXHR));
};

const reject = (deferred) => (jqXHR, _textStatus, _errorThrown) => {
  deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
};

const Agents = function (data) {
  Mixins.HasMany.call(this, {factory: Agents.Agent.create, as: 'Agent', collection: data, uniqueOn: 'uuid'});

  const agentsWithState = function (state) {
    return this.filterAgent((agent) => agent.agentConfigState() === state).length;
  };

  this.countDisabledAgents = function () {
    return agentsWithState.bind(this, 'Disabled')();
  };

  this.countEnabledAgents = function () {
    return agentsWithState.bind(this, 'Enabled')();
  };

  this.countPendingAgents = function () {
    return agentsWithState.bind(this, 'Pending')();
  };

  this.sortBy = function (attrName, order) {
    let sortedAgents;

    switch(attrName) {
    case 'agentState':
      sortedAgents = this.sortByAgents(statusComparator);
      break;
    case 'freeSpace':
      sortedAgents = this.sortByAgents(freeSpaceComparator);
      break;
    default:
      sortedAgents = this.sortByAgents(sortByAttrName(attrName));
    }
    
    if (order === 'desc') {
      sortedAgents = _.reverse(sortedAgents);
    }

    return new Agents(sortedAgents);
  };

  this.filterBy = function (text) {
    return new Agents(
      this.filterAgent((agent) => agent.matches(text))
    );
  };

  this.findAgentByUuid = function (uuid) {
    return this.findAgent((agent) => agent.uuid() === uuid);
  };

  this.disableAgents = (uuids) => {
    const json = {
      uuids,
      agent_config_state: 'Disabled'  //eslint-disable-line camelcase
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv4AgentsPath(),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(Agents.API_VERSION),
        data:        JSON.stringify(json),
        contentType: 'application/json'
      });

      jqXHR.then(resolve(deferred), reject(deferred));
    }).promise();
  };

  this.deleteAgents = (uuids) => {
    const json = {uuids};
    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'DELETE',
        url:         Routes.apiv4AgentsPath(),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(Agents.API_VERSION),
        data:        JSON.stringify(json),
        contentType: 'application/json'
      });

      jqXHR.then(resolve(deferred), reject(deferred));
    }).promise();
  };

  this.enableAgents = (uuids) => {
    const json = {
      uuids,
      agent_config_state: 'Enabled' //eslint-disable-line camelcase
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv4AgentsPath(),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(Agents.API_VERSION),
        data:        JSON.stringify(json),
        contentType: 'application/json'
      });

      jqXHR.then(resolve(deferred), reject(deferred));
    }).promise();
  };

  this.updateResources = (uuids, add, remove) => {
    const json = {
      uuids,
      operations: {
        resources: {add, remove}
      }
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv4AgentsPath(),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(Agents.API_VERSION),
        data:        JSON.stringify(json),
        contentType: 'application/json'
      });

      jqXHR.then(resolve(deferred), reject(deferred));
    }).promise();
  };

  this.updateEnvironments = (uuids, add, remove) => {
    const json = {
      uuids,
      operations: {environments: {add, remove}}
    };

    return $.Deferred(function () {
      const deferred = this;

      const jqXHR = $.ajax({
        method:      'PATCH',
        url:         Routes.apiv4AgentsPath(),
        timeout:     mrequest.timeout,
        beforeSend:  mrequest.xhrConfig.forVersion(Agents.API_VERSION),
        data:        JSON.stringify(json),
        contentType: 'application/json'
      });

      jqXHR.then(resolve(deferred), reject(deferred));
    }).promise();
  };

};

Agents.API_VERSION = 'v4';

CrudMixins.Index({
  type:     Agents,
  indexUrl: Routes.apiv4AgentsPath(),
  version:  Agents.API_VERSION,
  dataPath: '_embedded.agents'
});

const toHumanReadable = (freeSpace) => {
  try {
    if (_.isNumber(freeSpace)) {
      return filesize(freeSpace);
    } else {
      return 'Unknown';
    }
  } catch (e) {
    return 'Unknown';
  }
};

Agents.Agent = function (data) {
  const self             = this;
  this.uuid              = Stream(data.uuid);
  this.hostname          = Stream(data.hostname);
  this.ipAddress         = Stream(data.ipAddress);
  this.sandbox           = Stream(data.sandbox);
  this.operatingSystem   = Stream(data.operatingSystem);
  this.freeSpace         = Stream(data.freeSpace);
  this.readableFreeSpace = Stream(toHumanReadable(data.freeSpace));
  this.agentConfigState  = Stream(data.agentConfigState);
  this.agentState        = Stream(data.agentState);
  this.buildState        = Stream(data.buildState);
  this.resources         = Stream(s.defaultToIfBlank(data.resources, []));
  this.environments      = Stream(data.environments);
  this.buildDetails      = Stream(data.buildDetails);
  this.elasticAgentId    = Stream(data.elasticAgentId);
  this.elasticPluginId   = Stream(data.elasticPluginId);
  this.elasticAgentIcon  = Stream();
  this.parent            = Mixins.GetterSetter();

  this.status = function () {
    if (this.agentConfigState() === 'Pending') {
      return 'Pending';
    } else if (this.agentConfigState() === 'Disabled') {
      if (this.buildState() === 'Building') {
        return 'Disabled (Building)';
      } else if (this.buildState() === 'Cancelled') {
        return 'Disabled (Cancelled)';
      }
      return 'Disabled';
    } else if (this.agentState() === 'Building') {
      if (this.buildState() === 'Cancelled') {
        return 'Building (Cancelled)';
      }
      return 'Building';
    }
    return this.agentState();
  };

  this.matches = (filterText) => {
    const keys = ['hostname', 'operatingSystem', 'ipAddress', 'status', 'environments', 'resources', 'elasticAgentId', 'elasticPluginId'];
    filterText = filterText.toLowerCase();
    return _.some(keys, (field) => {
      const agentInfo = self[field]() ? self[field]().toString().toLowerCase() : '';
      return s.include(agentInfo, filterText);
    });
  };

  this.isElasticAgent = () => !_.isNil(self.elasticAgentId());

  this.toJSON = function () {
    return {
      hostname:           this.hostname(),
      resources:          this.resources(),
      environments:       this.environments(),
      agent_config_state: this.agentConfigState() //eslint-disable-line camelcase
    };
  };
};

Agents.Agent.BuildDetails = function (data) {
  this.isEmpty = Stream(_.isNil(data));
  if (this.isEmpty()) {
    return;
  }
  this.pipelineName = Stream(data.pipelineName);
  this.pipelineUrl  = Stream(data.pipelineUrl);
  this.stageName    = Stream(data.stageName);
  this.stageUrl     = Stream(data.stageUrl);
  this.jobName      = Stream(data.jobName);
  this.jobUrl       = Stream(data.jobUrl);
};

Agents.Agent.BuildDetails.fromJSON = (data) => {
  if (!data) {
    return new Agents.Agent.BuildDetails();
  } else {
    return new Agents.Agent.BuildDetails({
      pipelineName: data.pipeline_name,
      stageName:    data.stage_name,
      jobName:      data.job_name,
      pipelineUrl:  data._links.pipeline.href,
      stageUrl:     data._links.stage.href,
      jobUrl:       data._links.job.href,
    });
  }
};

Agents.Agent.fromJSON = (data) => new Agents.Agent({
  uuid:             data.uuid,
  hostname:         data.hostname,
  ipAddress:        data.ip_address,
  sandbox:          data.sandbox,
  operatingSystem:  data.operating_system,
  freeSpace:        data.free_space,
  agentConfigState: data.agent_config_state,
  agentState:       data.agent_state,
  buildState:       data.build_state,
  resources:        data.resources,
  environments:     data.environments,
  buildDetails:     Agents.Agent.BuildDetails.fromJSON(data.build_details),
  elasticAgentId:   data.elastic_agent_id,
  elasticPluginId:  data.elastic_plugin_id
});

Agents.Agent.create = (data) => new Agents.Agent(data);

Mixins.fromJSONCollection({
  parentType: Agents,
  childType:  Agents.Agent,
  via:        'addAgent'
});

module.exports = Agents;
