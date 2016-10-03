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

define(['mithril', 'lodash', 'string-plus',
  'models/model_mixins', 'filesize',
  'helpers/mrequest', 'js-routes', 'lodash-inflection'], function (m, _, s, Mixins, filesize, mrequest, Routes) {
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
  });

  var sortByAttrName = function (attrName) {
    return function (agent) {
      return _.toLower(agent[attrName]());
    };
  };

  var Agents = function (data) {
    this.disableAgents = function (uuids) {
      var json = {
        uuids:              uuids,
        agent_config_state: 'Disabled'  //eslint-disable-line camelcase
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv4AgentsPath(),
        config: mrequest.xhrConfig.v4,
        data:   json
      });
    };

    this.deleteAgents = function (uuids) {
      var json = {uuids: uuids};

      return m.request({
        method: 'DELETE',
        url:    Routes.apiv4AgentsPath(),
        config: mrequest.xhrConfig.v4,
        data:   json
      });
    };

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

    this.enableAgents = function (uuids) {
      var json = {
        uuids:              uuids,
        agent_config_state: 'Enabled' //eslint-disable-line camelcase
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv4AgentsPath(),
        config: mrequest.xhrConfig.v4,
        data:   json
      });
    };

    this.updateResources = function (uuids, add, remove) {
      var data = {
        uuids:      uuids,
        operations: {
          resources: {add: add, remove: remove}
        }
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv4AgentsPath(),
        config: mrequest.xhrConfig.v4,
        data:   data
      });
    };

    this.updateEnvironments = function (uuids, add, remove) {
      var data = {
        uuids:      uuids,
        operations: {environments: {add: add, remove: remove}}
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv4AgentsPath(),
        config: mrequest.xhrConfig.v4,
        data:   data
      });
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

    Mixins.HasMany.call(this, {factory: Agents.Agent.create, as: 'Agent', collection: data, uniqueOn: 'uuid'});
  };

  Agents.all = function (configCallBack) {
    return m.request({
      method:        "GET",
      url:           Routes.apiv4AgentsPath(),
      config:        function (xhr) {
        mrequest.xhrConfig.v4(xhr);
        if (configCallBack) {
          configCallBack(xhr);
        }
      },
      unwrapSuccess: function (data) {
        return Agents.fromJSON(data['_embedded']['agents']);
      }
    });
  };

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
    this.uuid              = m.prop(data.uuid);
    this.hostname          = m.prop(data.hostname);
    this.ipAddress         = m.prop(data.ipAddress);
    this.sandbox           = m.prop(data.sandbox);
    this.operatingSystem   = m.prop(data.operatingSystem);
    this.freeSpace         = m.prop(data.freeSpace);
    this.readableFreeSpace = m.prop(toHumanReadable(data.freeSpace));
    this.agentConfigState  = m.prop(data.agentConfigState);
    this.agentState        = m.prop(data.agentState);
    this.buildState        = m.prop(data.buildState);
    this.resources         = m.prop(s.defaultToIfBlank(data.resources, []));
    this.environments      = m.prop(data.environments);
    this.buildDetails      = m.prop(data.buildDetails);
    this.elasticAgentId    = m.prop(data.elasticAgentId);
    this.elasticPluginId   = m.prop(data.elasticPluginId);
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
    this.isEmpty = m.prop(_.isNil(data));
    if (this.isEmpty()) {
      return;
    }
    this.pipelineName = m.prop(data.pipelineName);
    this.pipelineUrl  = m.prop(data.pipelineUrl);
    this.stageName    = m.prop(data.stageName);
    this.stageUrl     = m.prop(data.stageUrl);
    this.jobName      = m.prop(data.jobName);
    this.jobUrl       = m.prop(data.jobUrl);
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

  return Agents;
});
