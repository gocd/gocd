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

describe('Agent Model', () => {

  const s = require('string-plus');
  const _ = require('lodash');
  require('jasmine-ajax');

  const Agents = require('models/agents/agents');

  it("should deserialize from json", () => {
    const agent = Agents.Agent.fromJSON(agentData[0]);
    expect(agent.uuid()).toBe('uuid-1');
    expect(agent.hostname()).toBe('host-1');
    expect(agent.ipAddress()).toBe('10.12.2.201');
    expect(agent.sandbox()).toBe('/var/lib/go-agent-2');
    expect(agent.operatingSystem()).toBe('Linux');
    expect(agent.freeSpace()).toBe(111902543872);
    expect(agent.agentConfigState()).toBe('Enabled');
    expect(agent.agentState()).toBe('Building');
    expect(agent.buildState()).toBe('Unknown');
    expect(agent.resources()).toEqual(['linux', 'java']);
    expect(agent.environments()).toEqual(['staging', 'perf']);
    expect(agent.buildDetails().pipelineUrl()).toEqual("http://localhost:8153/go/tab/pipeline/history/up42");
    expect(agent.buildDetails().stageUrl()).toEqual("http://localhost:8153/go/pipelines/up42/2/up42_stage/1");
    expect(agent.buildDetails().jobUrl()).toEqual("http://localhost:8153/go/tab/build/detail/up42/2/up42_stage/1/up42_job");
  });

  it("should serialize to JSON", () => {
    const agent = Agents.Agent.fromJSON(agentData[0]);

    expect(JSON.parse(JSON.stringify(agent, s.snakeCaser))).toEqual({
      hostname:           'host-1',
      resources:          ['linux', 'java'],
      environments:       ['staging', 'perf'],
      agent_config_state: 'Enabled' // eslint-disable-line camelcase
    });
  });

  it("should give the freeSpace in human readable format", () => {
    expect(new Agents.Agent({freeSpace: 1024}).readableFreeSpace()).toBe('1 KB');
    expect(new Agents.Agent({freeSpace: 2048526}).readableFreeSpace()).toBe('1.95 MB');
    expect(new Agents.Agent({freeSpace: 2199023255552}).readableFreeSpace()).toBe('2 TB');
    expect(new Agents.Agent({freeSpace: 'snafu'}).readableFreeSpace()).toBe('Unknown');
  });

  it("should count agents with specific state", () => {
    const agents = new Agents(
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

  describe("agent status", () => {
    it("should be pending when agentConfigState is Pending", () => {
      const agent = new Agents.Agent({agentConfigState: 'Pending'});
      expect(agent.status()).toBe('Pending');
    });

    it("should be 'Disabled (Building)' when agentConfigState is 'Disabled' and buildState is 'Building'", () => {
      const agent = new Agents.Agent({agentConfigState: 'Disabled', buildState: 'Building'});
      expect(agent.status()).toBe('Disabled (Building)');
    });

    it("should be 'Disabled (Cancelled)' when agentConfigState is 'Disabled' and buildState is 'Cancelled'", () => {
      const agent = new Agents.Agent({agentConfigState: 'Disabled', buildState: 'Cancelled'});
      expect(agent.status()).toBe('Disabled (Cancelled)');
    });

    it("should be 'Disabled' when agentConfigState is 'Disabled'", () => {
      const agent = new Agents.Agent({agentConfigState: 'Disabled'});
      expect(agent.status()).toBe('Disabled');
    });

    it("should be 'Building (Cancelled)' when agentState is 'Building' and buildState is 'Cancelled'", () => {
      const agent = new Agents.Agent({agentState: 'Building', buildState: 'Cancelled'});
      expect(agent.status()).toBe('Building (Cancelled)');
    });

    it("should be 'Building' when agentState is 'Building'", () => {
      const agent = new Agents.Agent({agentState: 'Building'});
      expect(agent.status()).toBe('Building');
    });
  });

  it('get all agents via ajax', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest('/go/api/agents').andReturn({
        "responseText":  JSON.stringify({
          "_embedded": {
            "agents": agentData
          }
        }),
        "status":        200,
        responseHeaders: {
          'Content-Type': 'application/vnd.go.cd.v4+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake((agents) => {
        expect(agents.countAgent()).toBe(9);
      });

      Agents.all().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  describe('agent matches', () => {
    it("should tell whether the specified string matches the agent's information", () => {
      const agent = Agents.Agent.fromJSON(agentData[0]);
      expect(agent.matches('host-1')).toBe(true);
      expect(agent.matches("Linux")).toBe(true);
      expect(agent.matches("10.12.2.201")).toBe(true);
      expect(agent.matches("linux")).toBe(true);
      expect(agent.matches("perf")).toBe(true);
      expect(agent.matches("invalid-search")).toBe(false);
    });

    it("should tell whether the specified elastic agent plugin id matches the agent's information", () => {
      const elasticAgentJSON = agentData[7];
      const agent            = Agents.Agent.fromJSON(elasticAgentJSON);
      expect(agent.matches(elasticAgentJSON.elastic_plugin_id)).toBe(true);
    });

    it("should tell whether the specified elastic agent id matches the agent's information", () => {
      const elasticAgentJSON = agentData[7];
      const agent            = Agents.Agent.fromJSON(elasticAgentJSON);
      expect(agent.matches(elasticAgentJSON.elastic_agent_id)).toBe(true);
    });
  });

  describe('sort the agents', () => {
    it("should sort based on OS", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('operatingSystem', 'asc');

      const operatingSystemsOfSortedAgents = sortedAgents.collectAgentProperty('operatingSystem');
      expect(operatingSystemsOfSortedAgents).toEqual(agents.collectAgentProperty('operatingSystem').sort());
    });

    it('should sort based on hostname', () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('hostname', 'asc');

      const hostnamesOfSortedAgents = sortedAgents.collectAgentProperty('hostname');
      expect(hostnamesOfSortedAgents).toEqual(agents.collectAgentProperty('hostname').sort());
    });

    it("should sort based on agent location", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('sandbox', 'asc');

      const sandboxesOfSortedAgents = sortedAgents.collectAgentProperty('sandbox');
      expect(sandboxesOfSortedAgents).toEqual(agents.collectAgentProperty('sandbox').sort());
    });

    it("should sort based on agent ip address", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('ipAddress', 'asc');

      const ipAddressesOfSortedAgents = sortedAgents.collectAgentProperty('ipAddress');
      expect(ipAddressesOfSortedAgents).toEqual(agents.collectAgentProperty('ipAddress').sort());
    });

    it("should sort based on agent status", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('agentState', 'asc');

      const statesOfSortedAgents = sortedAgents.collectAgentProperty('status');
      expect(statesOfSortedAgents).toEqual(["Pending", "LostContact", "Missing", "Building", "Building (Cancelled)", "Idle", "Disabled (Building)", "Disabled (Cancelled)", "Disabled"]);
    });

    it("should sort based on agent's free space", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('freeSpace', 'asc');
      const freeSpaceOfAgents = agents.collectAgentProperty('freeSpace');
      const expectedFreeSpacesOrder = _.sortBy(freeSpaceOfAgents, (space) => parseInt(space));

      const freeSpacesOfSortedAgents = sortedAgents.collectAgentProperty('freeSpace');
      expect(freeSpacesOfSortedAgents).toEqual(expectedFreeSpacesOrder);
    });

    it("should sort based on agent's resources", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('resources', 'asc');

      const resourcesOfSortedAgents = sortedAgents.collectAgentProperty('resources');
      expect(resourcesOfSortedAgents).toEqual(agents.collectAgentProperty('resources').sort());
    });

    it("should sort based on agent's environments", () => {
      const agents       = Agents.fromJSON(agentData);
      const sortedAgents = agents.sortBy('environments', 'asc');

      const environmentsOfSortedAgents = sortedAgents.collectAgentProperty('environments');
      expect(environmentsOfSortedAgents).toEqual(agents.collectAgentProperty('environments').sort());
    });
  });

  describe('elastic agent properties', () => {

    it("should have elastic agent id and elastic plugin id", () => {
      const elasticAgentData = agentData[7];
      const agent            = Agents.Agent.fromJSON(elasticAgentData);
      expect(agent.elasticAgentId()).toBe('0039ddc8-38a0-4f7b-be15-522fcb6f8649');
      expect(agent.elasticPluginId()).toBe('cd.go.contrib.elastic-agent.docker');
    });

    it("should be able to say elastic agent or not", () => {
      const elasticAgentData = agentData[7];
      const agent            = Agents.Agent.fromJSON(elasticAgentData);
      expect(agent.isElasticAgent()).toBeTruthy();
    });

    it("should default resource to be empty if no resource provided", () => {
      const elasticAgentData = agentData[7];
      const agent            = Agents.Agent.fromJSON(elasticAgentData);
      expect(agent.resources()).toEqual([]);
    });
  });

  const agentData = [
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-1',
      "hostname":           "host-1",
      "ip_address":         "10.12.2.201",
      "sandbox":            "/var/lib/go-agent-2",
      "operating_system":   "Linux",
      "free_space":         111902543872,
      "agent_config_state": "Enabled",
      "agent_state":        "Building",
      "build_state":        "Unknown",
      "resources":          [
        "linux", "java"
      ],
      "environments":       [
        "staging", "perf"
      ],
      "build_details":      {
        "_links":   {
          "job":      {
            "href": "http://localhost:8153/go/tab/build/detail/up42/2/up42_stage/1/up42_job"
          },
          "stage":    {
            "href": "http://localhost:8153/go/pipelines/up42/2/up42_stage/1"
          },
          "pipeline": {
            "href": "http://localhost:8153/go/tab/pipeline/history/up42"
          }
        },
        "pipeline": "up42",
        "stage":    "up42_stage",
        "job":      "up42_job"
      }
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-2',
      "hostname":           "host-4",
      "ip_address":         "10.12.2.200",
      "sandbox":            "/var/lib/go-agent-1",
      "operating_system":   "Windows",
      "free_space":         "unknown",
      "agent_config_state": "Pending",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          ["1111", "2222", "3333"],
      "environments":       []
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-3',
      "hostname":           "host-2",
      "ip_address":         "10.12.2.202",
      "sandbox":            "/var/lib/go-agent-3",
      "operating_system":   "Windows",
      "free_space":         0,
      "agent_config_state": "Enabled",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          ["zzzz"],
      "environments":       ['prod']
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-4',
      "hostname":           "host-0",
      "ip_address":         "10.12.2.203",
      "sandbox":            "/var/lib/go-agent-4",
      "operating_system":   "Windows",
      "free_space":         "unknown",
      "agent_config_state": "Enabled",
      "agent_state":        "LostContact",
      "build_state":        "Unknown",
      "resources":          [],
      "environments":       ['ci']
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-5',
      "hostname":           "host-10",
      "ip_address":         "10.12.2.204",
      "sandbox":            "/var/lib/go-agent-5",
      "operating_system":   "Mac OS X",
      "free_space":         10,
      "agent_config_state": "Disabled",
      "agent_state":        "Idle",
      "build_state":        "Cancelled",
      "resources":          [],
      "environments":       []
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-6',
      "hostname":           "host-11",
      "ip_address":         "10.12.2.204",
      "sandbox":            "/var/lib/go-agent-5",
      "operating_system":   "Mac OS X",
      "free_space":         "unknown",
      "agent_config_state": "Enabled",
      "agent_state":        "Idle",
      "build_state":        "Unknown",
      "resources":          [],
      "environments":       []
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-7',
      "hostname":           "host-12",
      "ip_address":         "10.12.2.204",
      "sandbox":            "/var/lib/go-agent-5",
      "operating_system":   "Mac OS X",
      "free_space":         "unknown",
      "agent_config_state": "Enabled",
      "agent_state":        "Building",
      "build_state":        "Cancelled",
      "resources":          [],
      "environments":       []
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-8',
      "hostname":           "host-13",
      "ip_address":         "10.12.2.204",
      "elastic_agent_id":   "0039ddc8-38a0-4f7b-be15-522fcb6f8649",
      "elastic_plugin_id":  "cd.go.contrib.elastic-agent.docker",
      "sandbox":            "/var/lib/go-agent-5",
      "operating_system":   "Mac OS X",
      "free_space":         "unknown",
      "agent_config_state": "Disabled",
      "agent_state":        "Building",
      "build_state":        "Building",
      "environments":       []
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               'uuid-9',
      "hostname":           "host-14",
      "ip_address":         "10.12.2.220",
      "sandbox":            "/var/lib/go-agent-10",
      "operating_system":   "Windows",
      "free_space":         "unknown",
      "agent_config_state": "Disabled",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          ["1111", "2222", "3333"],
      "environments":       []
    }
  ];
});
