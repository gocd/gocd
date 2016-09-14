/*
 * Copyright 2016 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

define([
  'mithril', 'lodash', 'string-plus',
  'models/model_mixins',
  'models/agents/agents',
], function (m, _, s, Mixin, Agents) {
  describe('Agent Model', function () {

    function ajaxCall(agentData) {
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({
        "responseText": JSON.stringify({
          "_embedded": {
            "agents": [agentData]
          }
        }),
        "status":       200
      });
    }

    beforeAll(function () {
      jasmine.Ajax.install();
      ajaxCall(agentData);
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
    });

    it("should deserialize from json", function () {
      var agent = Agents.Agent.fromJSON(agentData);
      expect(agent.uuid()).toBe('5fd3ac27-11bb-4fa2-95eb-7924040cb907');
      expect(agent.hostname()).toBe('Johnpkr.local');
      expect(agent.ipAddress()).toBe('127.0.0.1');
      expect(agent.sandbox()).toBe('/Users/bob/projects/gocd/gocd-master/agent');
      expect(agent.operatingSystem()).toBe('Mac OS X');
      expect(agent.freeSpace()).toBe(111902543872);
      expect(agent.agentConfigState()).toBe('Enabled');
      expect(agent.agentState()).toBe('LostContact');
      expect(agent.buildState()).toBe('Unknown');
      expect(agent.resources()).toEqual(['linux', 'java']);
      expect(agent.environments()).toEqual(['staging', 'perf']);
    });

    it("should serialize to JSON", function () {
      var agent = Agents.Agent.fromJSON(agentData);

      expect(JSON.parse(JSON.stringify(agent, s.snakeCaser))).toEqual({
        hostname:           'Johnpkr.local',
        resources:          ['linux', 'java'],
        environments:       ['staging', 'perf'],
        agent_config_state: 'Enabled' // eslint-disable-line camelcase
      });
    });


    it("should give the freeSpace in human readable format", function () {
      expect(new Agents.Agent({freeSpace: 1024}).readableFreeSpace()).toBe('1 KB');
      expect(new Agents.Agent({freeSpace: 2048526}).readableFreeSpace()).toBe('1.95 MB');
      expect(new Agents.Agent({freeSpace: 2199023255552}).readableFreeSpace()).toBe('2 TB');
      expect(new Agents.Agent({freeSpace: 'snafu'}).readableFreeSpace()).toBe('Unknown');
    });

    it("should count agents with specific state", function () {
      var agents = new Agents(
        [
          new Agents.Agent({agentConfigState: 'Pending'}),
          new Agents.Agent({agentConfigState: 'Disabled'}),
          new Agents.Agent({agentConfigState: 'Enabled'}),
        ]
      );

      expect(agents.countDisabledAgents()).toBe(1);
      expect(agents.countPendingAgents()).toBe(1);
      expect(agents.countEnabledAgents()).toBe(1);
    });

    describe("agent status", function () {
      it("should be pending when agentConfigState is Pending", function () {
        var agent = new Agents.Agent({agentConfigState: 'Pending'});
        expect(agent.status()).toBe('Pending');
      });

      it("should be 'Disabled (Building)' when agentConfigState is 'Disabled' and buildState is 'Building'", function () {
        var agent = new Agents.Agent({agentConfigState: 'Disabled', buildState: 'Building'});
        expect(agent.status()).toBe('Disabled (Building)');
      });

      it("should be 'Disabled (Cancelled)' when agentConfigState is 'Disabled' and buildState is 'Cancelled'", function () {
        var agent = new Agents.Agent({agentConfigState: 'Disabled', buildState: 'Cancelled'});
        expect(agent.status()).toBe('Disabled (Cancelled)');
      });

      it("should be 'Disabled' when agentConfigState is 'Disabled'", function () {
        var agent = new Agents.Agent({agentConfigState: 'Disabled'});
        expect(agent.status()).toBe('Disabled');
      });

      it("should be 'Building (Cancelled)' when agentState is 'Building' and buildState is 'Cancelled'", function () {
        var agent = new Agents.Agent({agentState: 'Building', buildState: 'Cancelled'});
        expect(agent.status()).toBe('Building (Cancelled)');
      });

      it("should be 'Building' when agentState is 'Building'", function () {
        var agent = new Agents.Agent({agentState: 'Building'});
        expect(agent.status()).toBe('Building');
      });
    });

    it('should give the agents object', function () {
      var agents = Agents.all();
      expect(agents().countAgent()).toBe(1);
    });

    describe('agent matches', function () {
      it("should tell whether the specified string matches the agent's information", function () {
        var agent = Agents.Agent.fromJSON(agentData);
        expect(agent.matches('Johnpkr')).toBe(true);
        expect(agent.matches("Mac OS X")).toBe(true);
        expect(agent.matches("127.0.0.1")).toBe(true);
        expect(agent.matches("linux")).toBe(true);
        expect(agent.matches("perf")).toBe(true);

        expect(agent.matches("invalid-search")).toBe(false);
      });
    });

    var agentData = {
      "_links":             {
        "self": {
          "href": "http://localhost:8153/go/api/agents/5fd3ac27-11bb-4fa2-95eb-7924040cb907"
        },
        "doc":  {
          "href": "http://api.go.cd/#agents"
        },
        "find": {
          "href": "http://localhost:8153/go/api/agents/:uuid"
        }
      },
      "uuid":               "5fd3ac27-11bb-4fa2-95eb-7924040cb907",
      "hostname":           "Johnpkr.local",
      "ip_address":         "127.0.0.1",
      "sandbox":            "/Users/bob/projects/gocd/gocd-master/agent",
      "operating_system":   "Mac OS X",
      "free_space":         111902543872,
      "agent_config_state": "Enabled",
      "agent_state":        "LostContact",
      "build_state":        "Unknown",
      "resources":          [
        'linux',
        'java'
      ],
      "environments":       [
        'staging',
        'perf'
      ]
    };

  });
});
