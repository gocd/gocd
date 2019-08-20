/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import {Agent, AgentConfigState, Agents, AgentsEnvironment, AgentState, BuildState} from "models/new-agent/agents";
import {AgentsTestData} from "models/new-agent/spec/agents_test_data";

describe("AgentsModel", () => {
  describe("Deserialize", () => {
    it("should deserialize all agents from JSON", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());

      expect(agents.count()).toBe(3);
    });

    it("should deserialize agent from JSON", () => {
      const agentJSON = AgentsTestData.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23", "1773fcad85d8");
      const agent     = Agent.fromJSON(agentJSON);

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
      const agentJSON          = AgentsTestData.agent("45583959-97e2-4b8e-8eab-2f98d73f2e23",
                                                      "1773fcad85d8",
                                                      undefined,
                                                      undefined,
                                                      "Building",
                                                      agentFreeSpaceInGB);

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.readableFreeSpace()).toEqual("5.5 GB");

    });

    it("should return environments", () => {
      const agentJSON = AgentsTestData.agentWithEnvironments("prod", "dev", "qa");

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.environments).toHaveLength(3);
      expect(agent.environments[0]).toEqual(new AgentsEnvironment("prod", "gocd"));
      expect(agent.environments[1]).toEqual(new AgentsEnvironment("dev", "gocd"));
      expect(agent.environments[2]).toEqual(new AgentsEnvironment("qa", "gocd"));
    });

    it("should return environments name", () => {
      const agentJSON = AgentsTestData.agentWithEnvironments("prod", "dev", "qa");

      const agent = Agent.fromJSON(agentJSON);

      expect(agent.environmentNames()).toHaveLength(3);
      expect(agent.environmentNames()[0]).toEqual("prod");
      expect(agent.environmentNames()[1]).toEqual("dev");
      expect(agent.environmentNames()[2]).toEqual("qa");
    });

    it("should return empty array when agent is not associated with any environments", () => {
      const agentJSON = AgentsTestData.agentWithEnvironments();

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
  });

  describe("InitializeWith", () => {
    it("should have all the agents", () => {
      const agents = new Agents([]);
      expect(agents.list()).toHaveLength(0);

      agents.initializeWith(Agents.fromJSON(AgentsTestData.list()));

      expect(agents.list()).toHaveLength(3);
    });

    it("should remove previously known agent which is now unknown", () => {
      const previouslyKnownAgent = Agent.fromJSON(AgentsTestData.agent("agent-a", "host-a"));
      const agents               = new Agents([previouslyKnownAgent]);
      expect(agents.list()).toHaveLength(1);
      expect(agents.isKnownAgent(previouslyKnownAgent.uuid)).toBeTruthy();

      agents.initializeWith(Agents.fromJSON(AgentsTestData.list()));

      expect(agents.list()).toHaveLength(3);
      expect(agents.isKnownAgent(previouslyKnownAgent.uuid)).toBeFalsy();
    });

    it("should persist agent selection", () => {
      const originalAgentsJSON = AgentsTestData.list();
      const agents             = Agents.fromJSON(originalAgentsJSON);
      agents.list()[0].selected(true);

      expect(agents.list()[0].selected()).toBeTruthy();
      expect(agents.list()[1].selected()).toBeFalsy();
      expect(agents.list()[2].selected()).toBeFalsy();

      agents.initializeWith(Agents.fromJSON(originalAgentsJSON));

      expect(agents.list()[0].selected()).toBeTruthy();
      expect(agents.list()[1].selected()).toBeFalsy();
      expect(agents.list()[2].selected()).toBeFalsy();
    });
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

  describe("Sort", () => {

    it("should get the sortable column index", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());

      expect(agents.getSortableColumns()).toEqual([1, 2, 3, 4, 5, 6, 7, 8]);
    });

    it("should sort the data based on the column clicked", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(2);

      expect(agents.list()).toEqual([agentB, agentC, agentA]);
    });

    it("should sort the data in reverser order when the same column is clicked", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(2);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(2);
      const agentsToRepresentAfterClickOnSameColumn = agents.list();
      const expectedAfterClickOnSameColumn          = [agentA, agentC, agentB];
      expect(agentsToRepresentAfterClickOnSameColumn).toEqual(expectedAfterClickOnSameColumn);
    });

    it("should always sort a new column in ascending order regardless of previous column's sort order", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(2);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(1);
      const agentsToRepresentAfterClickOnAnotherColumn = agents.list();
      const expectedAfterClickOnAnotherColumn          = [agentA, agentB, agentC];
      expect(agentsToRepresentAfterClickOnAnotherColumn).toEqual(expectedAfterClickOnAnotherColumn);
    });

    it("should sort a column according to agent state in ascending order when click on status column", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(5);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentA, agentC, agentB];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);
    });

    it("should sort a column according to agent state in descending order when click on status column twice", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(5);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentA, agentC, agentB];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(5);
      const agentsToRepresentAfterSecondClick = agents.list();
      const expectedAfterSecondClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterSecondClick).toEqual(expectedAfterSecondClick);
    });

    it("should sort a column according to free space in ascending order when click on free space column", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(6);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);
    });

    it("should sort a column according to free space in descending order when click on free space column twice", () => {
      const agents = Agents.fromJSON(AgentsTestData.list());
      const agentA = agents.list()[0];
      const agentB = agents.list()[1];
      const agentC = agents.list()[2];

      expect(agents.list()).toEqual([agentA, agentB, agentC]);

      agents.onColumnClick(6);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentC, agentA];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(6);
      const agentsToRepresentAfterSecondClick = agents.list();
      const expectedAfterSecondClick          = [agentA, agentC, agentB];
      expect(agentsToRepresentAfterSecondClick).toEqual(expectedAfterSecondClick);
    });

    it("should sort a column according to resources in ascending order when click on resource column", () => {
      const agentA = Agent.fromJSON(AgentsTestData.agentWithResources(["dev", "fat"]));
      const agentB = Agent.fromJSON(AgentsTestData.agentWithResources(["firefox"]));
      const agentC = Agent.fromJSON(AgentsTestData.agentWithResources(["chrome"]));
      const agentD = Agent.fromJSON(AgentsTestData.agentWithResources([]));

      const agents = new Agents([agentA, agentB, agentC, agentD]);

      expect(agents.list()).toEqual([agentA, agentB, agentC, agentD]);

      agents.onColumnClick(7);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentC, agentA, agentB, agentD];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);
    });

    it("should sort a column according to resources in descending order when click on resource column twice", () => {
      const agentA = Agent.fromJSON(AgentsTestData.agentWithResources(["dev", "fat"]));
      const agentB = Agent.fromJSON(AgentsTestData.agentWithResources(["firefox"]));
      const agentC = Agent.fromJSON(AgentsTestData.agentWithResources(["chrome"]));
      const agentD = Agent.fromJSON(AgentsTestData.agentWithResources([]));

      const agents = new Agents([agentA, agentB, agentC, agentD]);

      expect(agents.list()).toEqual([agentA, agentB, agentC, agentD]);

      agents.onColumnClick(7);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentC, agentA, agentB, agentD];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

      agents.onColumnClick(7);
      const agentsToRepresentAfterSecondClick = agents.list();
      const expectedAfterSecondClick          = [agentD, agentB, agentA, agentC];
      expect(agentsToRepresentAfterSecondClick).toEqual(expectedAfterSecondClick);

    });

    it("should sort a column according to environment name in ascending order when click on environment column", () => {
      const agentA = Agent.fromJSON(AgentsTestData.agentWithEnvironments("prod", "dev", "qa"));
      const agentB = Agent.fromJSON(AgentsTestData.agentWithEnvironments("dev"));
      const agentC = Agent.fromJSON(AgentsTestData.agentWithEnvironments("qa"));
      const agentD = Agent.fromJSON(AgentsTestData.agentWithEnvironments());

      const agents = new Agents([agentA, agentB, agentC, agentD]);

      expect(agents.list()).toEqual([agentA, agentB, agentC, agentD]);

      agents.onColumnClick(8);
      const agentsToRepresentAfterClick = agents.list();
      const expectedAfterClick          = [agentB, agentA, agentC, agentD];
      expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);
    });

    it("should sort a column according to environment name in descending order when click on environment column twice",
       () => {
         const agentA = Agent.fromJSON(AgentsTestData.agentWithEnvironments("prod", "dev", "qa"));
         const agentB = Agent.fromJSON(AgentsTestData.agentWithEnvironments("dev"));
         const agentC = Agent.fromJSON(AgentsTestData.agentWithEnvironments("qa"));
         const agentD = Agent.fromJSON(AgentsTestData.agentWithEnvironments());

         const agents = new Agents([agentA, agentB, agentC, agentD]);

         expect(agents.list()).toEqual([agentA, agentB, agentC, agentD]);

         agents.onColumnClick(8);
         const agentsToRepresentAfterClick = agents.list();
         const expectedAfterClick          = [agentB, agentA, agentC, agentD];
         expect(agentsToRepresentAfterClick).toEqual(expectedAfterClick);

         agents.onColumnClick(8);
         const agentsToRepresentAfterSecondClick = agents.list();
         const expectedAfterSecondClick          = [agentD, agentC, agentA, agentB];
         expect(agentsToRepresentAfterSecondClick).toEqual(expectedAfterSecondClick);
       });
  });

  describe("Search", () => {
    let agents: Agents, agentA: Agent, agentB: Agent, agentC: Agent;
    beforeEach(() => {
      agents = Agents.fromJSON(AgentsTestData.list());
      agentA = agents.list()[0];
      agentB = agents.list()[1];
      agentC = agents.list()[2];
    });

    it("should search agents based on hostname", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("windows");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });

    it("should search agents based on sandbox", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("Xx");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentB]);
    });

    it("should search agents based on os", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("mac");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentC]);
    });

    it("should search agents based on ip address", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText(".5");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });

    it("should search agents based on free space", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("2859");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentB]);
    });

    it("should search agents based on resources", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("fire");

      expect(agents.list().length).toEqual(3);
      expect(agents.list()).toEqual([agentA, agentB, agentC]);
    });

    it("should search agents based on environments", () => {
      const agentA = Agent.fromJSON(AgentsTestData.agentWithEnvironments("test", "qa")),
            agentB = Agent.fromJSON(AgentsTestData.agentWithEnvironments("prod")),
            agentC = Agent.fromJSON(AgentsTestData.agentWithEnvironments("qa"));
      const agents = new Agents([agentA, agentB, agentC]);

      expect(agents.list().length).toEqual(3);

      agents.filterText("test");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
    });

    it("should search agents case-insensitively", () => {
      expect(agents.list().length).toEqual(3);

      agents.filterText("WinDow");

      expect(agents.list().length).toEqual(1);
      expect(agents.list()).toEqual([agentA]);
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

  describe("Agent selection", () => {
    describe("AreAllFilteredAgentsSelected", () => {
      it("should return false if no agent is selected", () => {
        const agents = Agents.fromJSON(AgentsTestData.list());

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
      });

      it("should return true if all agents are selected with given filter", () => {
        const agentA = Agent.fromJSON(AgentsTestData.idleAgent()),
              agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
              agentC = Agent.fromJSON(AgentsTestData.pendingAgent());
        const agents = new Agents([agentA, agentB, agentC]);

        agents.filterText("Building");
        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();

        agentB.selected(true);

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
      });

      it("should return true if all agents are selected and filter is not applied", () => {
        const agents = Agents.fromJSON(AgentsTestData.list()),
              agentA = agents.list()[0],
              agentB = agents.list()[1],
              agentC = agents.list()[2];

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();

        agentA.selected(true);
        agentB.selected(true);
        agentC.selected(true);

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
      });

      it("should return false if any agent is not selected", () => {
        const agents = Agents.fromJSON(AgentsTestData.list());
        const agentA = agents.list()[0];
        const agentC = agents.list()[2];

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();

        agentA.selected(true);
        agentC.selected(true);

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
      });
    });

    describe("ToggleFilteredAgentsSelection", () => {
      it("should select all the agents when none of the agent is selected when not filtered", () => {
        const agents = Agents.fromJSON(AgentsTestData.list()),
              agentA = agents.list()[0],
              agentB = agents.list()[1],
              agentC = agents.list()[2];

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
        expect(agentA.selected()).toBeTruthy();
        expect(agentB.selected()).toBeTruthy();
        expect(agentC.selected()).toBeTruthy();
      });

      it("should select all the agents when part of the agents are selected when no filter applied", () => {
        const agents = Agents.fromJSON(AgentsTestData.list()),
              agentA = agents.list()[0],
              agentB = agents.list()[1],
              agentC = agents.list()[2];
        agentA.selected(true);
        agentB.selected(true);
        expect(agentC.selected()).toBeFalsy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
        expect(agentA.selected()).toBeTruthy();
        expect(agentB.selected()).toBeTruthy();
        expect(agentC.selected()).toBeTruthy();
      });

      it("should deselect all the agents when all the agents are selected when no filter applied", () => {
        const agents = Agents.fromJSON(AgentsTestData.list()),
              agentA = agents.list()[0],
              agentB = agents.list()[1],
              agentC = agents.list()[2];
        agentA.selected(true);
        agentB.selected(true);
        agentC.selected(true);

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
        expect(agentA.selected()).toBeFalsy();
        expect(agentB.selected()).toBeFalsy();
        expect(agentC.selected()).toBeFalsy();
      });

      it("should select all the agents when none of the agent is selected when filter applied", () => {
        const agentA = Agent.fromJSON(AgentsTestData.idleAgent()),
              agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
              agentC = Agent.fromJSON(AgentsTestData.pendingAgent());
        const agents = new Agents([agentA, agentB, agentC]);

        agents.filterText("Building");
        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
        expect(agentA.selected()).toBeFalsy();
        expect(agentB.selected()).toBeFalsy();
        expect(agentC.selected()).toBeFalsy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
        expect(agentB.selected()).toBeTruthy();
        expect(agentA.selected()).toBeFalsy();
        expect(agentC.selected()).toBeFalsy();
      });

      it("should persist the selected agent before applying filter", () => {
        const agentA = Agent.fromJSON(AgentsTestData.idleAgent()),
              agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
              agentC = Agent.fromJSON(AgentsTestData.pendingAgent());
        const agents = new Agents([agentA, agentB, agentC]);
        agentA.selected(true);

        agents.filterText("Building");
        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
        expect(agentA.selected()).toBeTruthy();
        expect(agentB.selected()).toBeFalsy();
        expect(agentC.selected()).toBeFalsy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeTruthy();
        expect(agentA.selected()).toBeTruthy();
        expect(agentB.selected()).toBeTruthy();
        expect(agentC.selected()).toBeFalsy();

        agents.toggleFilteredAgentsSelection();

        expect(agents.areAllFilteredAgentsSelected()).toBeFalsy();
        expect(agentA.selected()).toBeTruthy();
        expect(agentB.selected()).toBeFalsy();
        expect(agentC.selected()).toBeFalsy();
      });
    });
  });

  describe("FilterByAgentConfigState", () => {
    it("should return all agents by agent config state", () => {
      const agentA = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agents = new Agents([agentA, agentB, agentC, agentD, agentE]);

      let filteredAgents = agents.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toContain(agentC, agentE);

      filteredAgents = agents.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(2);
      expect(filteredAgents).toContain(agentB, agentD);

      filteredAgents = agents.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentA);
    });

    it("should return agents by agent config state after applying search filter", () => {
      const agentA = Agent.fromJSON(AgentsTestData.disabledAgent()),
            agentB = Agent.fromJSON(AgentsTestData.buildingAgent()),
            agentC = Agent.fromJSON(AgentsTestData.pendingAgent()),
            agentD = Agent.fromJSON(AgentsTestData.idleAgent()),
            agentE = Agent.fromJSON(AgentsTestData.pendingAgent());
      const agents = new Agents([agentA, agentB, agentC, agentD, agentE]);

      agents.filterText("Building");

      let filteredAgents = agents.filterBy(AgentConfigState.Pending);
      expect(filteredAgents).toHaveLength(0);

      filteredAgents = agents.filterBy(AgentConfigState.Enabled);
      expect(filteredAgents).toHaveLength(1);
      expect(filteredAgents).toContain(agentB);

      filteredAgents = agents.filterBy(AgentConfigState.Disabled);
      expect(filteredAgents).toHaveLength(0);
    });
  });
});
