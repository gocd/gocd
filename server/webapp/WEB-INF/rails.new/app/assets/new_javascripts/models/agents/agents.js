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
  'helpers/mrequest', 'lodash-inflection', 'js-routes'], function (m, _, s, Mixins, filesize, mrequest, JsRoutes) {
  var Agents = function (data) {

    this.disableAgents = function (uuids) {
      var json = {
        uuids:              uuids,
        agent_config_state: 'Disabled'
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv2AgentsPath(),
        config: mrequest.xhrConfig.v2,
        data:   json
      });
    };

    this.deleteAgents = function (uuids) {
      var json = {uuids: uuids};

      return m.request({
        method: 'DELETE',
        url:    Routes.apiv2AgentsPath(),
        config: mrequest.xhrConfig.v2,
        data:   json
      });
    };

    this.enableAgents = function (uuids) {
      var json = {
        uuids:              uuids,
        agent_config_state: 'Enabled'
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv2AgentsPath(),
        config: mrequest.xhrConfig.v2,
        data:   json
      });
    };

    this.updateResources = function (uuids, add, remove, message, type, success) {
      var data = {
        uuids:      uuids,
        operations: {
          resources: {add: add, remove: remove}
        }
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv2AgentsPath(),
        config: mrequest.xhrConfig.v2,
        data:   data
      })
    };

    this.updateEnvironments = function (uuids, add, remove, message, type, success) {
      var data = {
        uuids:      uuids,
        operations: {environments: {add: add, remove: remove}}
      };

      return m.request({
        method: 'PATCH',
        url:    Routes.apiv2AgentsPath(),
        config: mrequest.xhrConfig.v2,
        data:   data
      });
    };

    Mixins.HasMany.call(this, {factory: Agents.Agent.create, as: 'Agent', collection: data, uniqueOn: 'uuid'});
  };

  Agents.all = function (configCallBack) {
    return m.request({
      method:        "GET",
      url:           Routes.apiv2AgentsPath(),
      config:        function (xhr) {
        mrequest.xhrConfig.v3(xhr);
        if (configCallBack) {
          configCallBack(xhr)
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
        return 'Unknown'
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
    this.resources         = m.prop(data.resources);
    this.environments      = m.prop(data.environments);
    this.parent            = Mixins.GetterSetter();

    this.status = function () {
      if (this.agentConfigState() === 'Pending') {
        return 'Pending';
      } else if (this.agentConfigState() === 'Disabled') {
        if (this.buildState() == 'Building') {
          return 'Disabled (Building)';
        } else if (this.buildState() == 'Cancelled') {
          return 'Disabled (Cancelled)';
        }
        return 'Disabled';
      } else if (this.agentState() == 'Building') {
        if (this.buildState() == 'Cancelled') {
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

    this.toJSON = function () {
      return {
        hostname:           this.hostname(),
        resources:          this.resources(),
        environments:       this.environments(),
        agent_config_state: this.agentConfigState()
      }
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
      environments:     data.environments
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
