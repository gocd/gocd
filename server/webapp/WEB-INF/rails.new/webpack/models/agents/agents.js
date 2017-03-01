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

var $          = require('jquery');
var Stream     = require('mithril/stream');
var _          = require('lodash');
var s          = require('string-plus');
var Mixins     = require('models/mixins/model_mixins');
var filesize   = require('filesize');
var mrequest   = require('helpers/mrequest');
var Routes     = require('gen/js-routes');
var CrudMixins = require('models/mixins/crud_mixins');

require('lodash-inflection');

var resolver = function (agent) {
  return agent.uuid() + agent.status();
};

var statusComparator = _.memoize(function (agent) {
  var rank = {
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
}, resolver);

var sortByAttrName = function (attrName) {
  return function (agent) {
    return _.toLower(agent[attrName]());
  };
};

var resolve = function (deferred) {
  return function (data, _textStatus, jqXHR) {
    deferred.resolve(mrequest.unwrapMessage(data, jqXHR));
  };
};

var reject = function (deferred) {
  return function (jqXHR, _textStatus, _errorThrown) {
    deferred.reject(mrequest.unwrapErrorExtractMessage(jqXHR.responseJSON, jqXHR));
  };
};

var Agents = function (data) {
  Mixins.HasMany.call(this, {factory: Agents.Agent.create, as: 'Agent', collection: data, uniqueOn: 'uuid'});

  var agentsWithState = _.memoize(function (state) {
    return this.filterAgent(function (agent) {
      return agent.agentConfigState() === state;
    }).length;
  });

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
    var sortedAgents;

    if (attrName === 'agentState') {
      sortedAgents = this.sortByAgents(statusComparator);
    } else {
      sortedAgents = this.sortByAgents(sortByAttrName(attrName));
    }

    if (order === 'desc') {
      sortedAgents = _.reverse(sortedAgents);
    }

    return new Agents(sortedAgents);
  };

  this.filterBy = function (text) {
    return new Agents(
      this.filterAgent(function (agent) {
        return agent.matches(text);
      })
    );
  };

  this.findAgentByUuid = function (uuid) {
    return this.findAgent(function (agent) {
      return agent.uuid() === uuid;
    });
  };

  this.disableAgents = function (uuids) {
    var json = {
      uuids:              uuids,
      agent_config_state: 'Disabled'  //eslint-disable-line camelcase
    };

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
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

  this.deleteAgents = function (uuids) {
    var json = {uuids: uuids};
    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
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

  this.enableAgents = function (uuids) {
    var json = {
      uuids:              uuids,
      agent_config_state: 'Enabled' //eslint-disable-line camelcase
    };

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
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

  this.updateResources = function (uuids, add, remove) {
    var json = {
      uuids:      uuids,
      operations: {
        resources: {add: add, remove: remove}
      }
    };

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
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

  this.updateEnvironments = function (uuids, add, remove) {
    var json = {
      uuids:      uuids,
      operations: {environments: {add: add, remove: remove}}
    };

    return $.Deferred(function () {
      var deferred = this;

      var jqXHR = $.ajax({
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

var toHumanReadable = function (freeSpace) {
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
  var self               = this;
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

  this.matches = function (filterText) {
    var keys   = ['hostname', 'operatingSystem', 'ipAddress', 'status', 'environments', 'resources'];
    filterText = filterText.toLowerCase();
    return _.some(keys, function (field) {
      var agentInfo = self[field]().toString().toLowerCase();
      return s.include(agentInfo, filterText);
    });
  };

  this.isElasticAgent = function () {
    return !_.isNil(self.elasticAgentId());
  };

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

Agents.Agent.BuildDetails.fromJSON = function (data) {
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

Agents.Agent.fromJSON = function (data) {
  return new Agents.Agent({
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
};

Agents.Agent.create = function (data) {
  return new Agents.Agent(data);
};

Mixins.fromJSONCollection({
  parentType: Agents,
  childType:  Agents.Agent,
  via:        'addAgent'
});

module.exports = Agents;
