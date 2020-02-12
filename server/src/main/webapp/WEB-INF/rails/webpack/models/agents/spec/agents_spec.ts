/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import {
  Agent,
  AgentConfigState,
  Agents,
  AgentsEnvironment,
  AgentState, BuildState
} from "models/agents/agents";
import {AgentsTestData} from "models/agents/spec/agents_test_data";

describe("AgentModel", () => {
  it("should deserialize to static agent", () => {
    const agentJSON = AgentsTestData.idleAgent();

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.uuid).toEqual(agentJSON.uuid);
    expect(agent.hostname).toEqual(agentJSON.hostname);
    expect(agent.ipAddress).toEqual(agentJSON.ip_address);
    expect(agent.sandbox).toEqual(agentJSON.sandbox);
    expect(agent.operatingSystem).toEqual(agentJSON.operating_system);
    expect(agent.agentConfigState).toEqual(AgentConfigState.Enabled);
    expect(agent.agentState).toEqual(AgentState.Idle);
    expect(agent.buildState).toEqual(BuildState.Idle);
    expect(agent.freeSpace).toEqual(agentJSON.free_space);
    expect(agent.resources).toEqual(agentJSON.resources);
  });

  it("should return free space in human readable format", () => {
    const agentFreeSpaceInGB = 5.5 * 1024 * 1024 * 1024;
    const agentJSON          = AgentsTestData.withFreespace(agentFreeSpaceInGB);

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.readableFreeSpace()).toEqual("5.5 GB");
  });

  it("should return environments", () => {
    const agentJSON = AgentsTestData.withEnvironments("prod", "dev", "qa");

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.environments).toHaveLength(3);
    expect(agent.environments[0]).toEqual(new AgentsEnvironment("prod", "gocd"));
    expect(agent.environments[1]).toEqual(new AgentsEnvironment("dev", "gocd"));
    expect(agent.environments[2]).toEqual(new AgentsEnvironment("qa", "gocd"));
  });

  it("should return environments name", () => {
    const agentJSON = AgentsTestData.withEnvironments("prod", "dev", "qa");

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.environmentNames()).toHaveLength(3);
    expect(agent.environmentNames()[0]).toContain("prod");
    expect(agent.environmentNames()[1]).toEqual("dev");
    expect(agent.environmentNames()[2]).toEqual("qa");
  });

  it("should return empty array when agent is not associated with any environments", () => {
    const agentJSON = AgentsTestData.withEnvironments();

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.environmentNames()).toHaveLength(0);
  });

  it("should deserialize build details", () => {
    const agentJSON = AgentsTestData.buildingAgent();

    const agent = Agent.fromJSON(agentJSON);

    expect(agent.buildDetails!.pipelineName).toEqual("up42");
    expect(agent.buildDetails!.stageName).toEqual("up42_stage");
    expect(agent.buildDetails!.jobName).toEqual("up42_job");
    expect(agent.buildDetails!.pipelineUrl).toEqual("pipeline_url");
    expect(agent.buildDetails!.stageUrl).toEqual("stage_url");
    expect(agent.buildDetails!.jobUrl).toEqual("job_url");
  });

  describe("isBuilding", () => {
    it("should be true if building", () => {
      const agentJSON = AgentsTestData.buildingAgent();

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.isBuilding()).toBeTruthy();
    });

    it("should be false if not building", () => {
      const agentJSON = AgentsTestData.idleAgent();

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.isBuilding()).toBeFalsy();
    });
  });

  describe("Agent Status", () => {
    it("should show as pending when agent is in pending state", () => {
      const agentInPendingState = Agent.fromJSON(AgentsTestData.pendingAgent());

      const agentStatus = agentInPendingState.status();

      expect(agentStatus).toEqual("Pending");
    });

    describe("Enabled", () => {
      it("should show as Building (Cancelled) when agent is enabled and job is cancelled", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Building (Cancelled)");
      });

      it("should show as Building when agent is enabled and building a job", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.buildingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Building");
      });

      it("should show as Idle when agent is idle", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.idleAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Idle");
      });

      it("should show as Missing when agent is in missing state", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.missingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Missing");
      });

      it("should show as LostContact when agent is in lost contact state", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.lostContactAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("LostContact");
      });
    });

    describe("Disabled", () => {
      it("should show as Disabled(Building) when agent is disabled and job is running", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledBuildingAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled (Building)");
      });

      it("should show as Disabled(Cancelled) when agent is disabled and job is cancelled", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledCancelledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled (Cancelled)");
      });

      it("should show as Disabled when agent is disabled and job is not running", () => {
        const agentInPendingState = Agent.fromJSON(AgentsTestData.disabledAgent());

        const agentStatus = agentInPendingState.status();

        expect(agentStatus).toEqual("Disabled");
      });
    });
  });

  describe("isElastic", () => {
    it("should be false if agent is a normal agent", () => {
      const agentJSON = AgentsTestData.idleAgent();
      const agent     = Agent.fromJSON(agentJSON);

      expect(agent.isElastic()).toBeFalsy();
    });

    it("should be false if elastic agent id is not present", () => {
      const agentJSON = AgentsTestData.elasticAgent();
      delete agentJSON.elastic_agent_id;

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.isElastic()).toBeFalsy();
    });

    it("should be false if elastic plugin id is not present", () => {
      const agentJSON = AgentsTestData.elasticAgent();
      delete agentJSON.elastic_plugin_id;

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.isElastic()).toBeFalsy();
    });

    it("should be true if agent is an elastic agent", () => {
      const agentJSON = AgentsTestData.elasticAgent();
      const agent     = Agent.fromJSON(agentJSON);

      expect(agent.isElastic()).toBeTruthy();
    });
  });
});

describe("NewAgentsModel", () => {
  let idleAgent: Agent, buildingAgent: Agent, disabledAgent: Agent, agents: Agents;
  beforeEach(() => {
    idleAgent     = Agent.fromJSON(AgentsTestData.idleAgent());
    buildingAgent = Agent.fromJSON(AgentsTestData.buildingAgent());
    disabledAgent = Agent.fromJSON(AgentsTestData.disabledAgent());
    agents        = new Agents(idleAgent, buildingAgent, disabledAgent);
  });

  it("should deserialize all agents from JSON", () => {
    const agents = Agents.fromJSON(AgentsTestData.list());
    expect(agents).toHaveLength(3);
  });

  describe("hasAgent", () => {
    it("should be false if agent with uuid does not exist", () => {
      expect(agents.hasAgent("unknown-agent-id")).toBeFalsy();
    });

    it("should be true if agent with uuid exist", () => {
      expect(agents.hasAgent(agents[1].uuid)).toBeTruthy();
    });
  });
});
