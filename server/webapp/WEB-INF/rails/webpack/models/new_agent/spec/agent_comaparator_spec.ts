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

import {AgentComparator, SortOrder} from "models/new_agent/agent_comparator";
import {Agent} from "models/new_agent/agents";
import {AgentsTestData} from "models/new_agent/spec/agents_test_data";

describe("AgentComparator", () => {
  let originalOrder: Agent[];

  describe("Agent Status", () => {
    let idle: Agent, pending: Agent, disabled: Agent, building: Agent, disabledBuilding: Agent, disabledCanceled: Agent,
        lostContact: Agent, buildingCancelled: Agent, missing: Agent;
    beforeEach(() => {
      idle              = Agent.fromJSON(AgentsTestData.idleAgent());
      pending           = Agent.fromJSON(AgentsTestData.pendingAgent());
      disabled          = Agent.fromJSON(AgentsTestData.disabledAgent());
      building          = Agent.fromJSON(AgentsTestData.buildingAgent());
      disabledBuilding  = Agent.fromJSON(AgentsTestData.disabledBuildingAgent());
      disabledCanceled  = Agent.fromJSON(AgentsTestData.disabledCancelledAgent());
      lostContact       = Agent.fromJSON(AgentsTestData.lostContactAgent());
      buildingCancelled = Agent.fromJSON(AgentsTestData.buildingCancelledAgent());
      missing           = Agent.fromJSON(AgentsTestData.missingAgent());
      originalOrder     = [idle, pending, disabled, building, disabledBuilding, disabledCanceled, lostContact, buildingCancelled, missing];
    });

    it("should sort agents in ascending order", () => {
      const comparator = new AgentComparator("agentState", SortOrder.ASC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      const expectedAfterSort = [pending, lostContact, missing, building, buildingCancelled, idle, disabledBuilding, disabledCanceled, disabled];
      expect(sortedList).toEqual(expectedAfterSort);
    });

    it("should sort agents in descending order", () => {
      const comparator = new AgentComparator("agentState", SortOrder.DESC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      const expectedAfterSort = [disabled, disabledCanceled, disabledBuilding, idle, buildingCancelled, building, missing, lostContact, pending];
      expect(sortedList).toEqual(expectedAfterSort);
    });
  });

  describe("Free Space", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.agentWithFreespace(10000));
      agentB        = Agent.fromJSON(AgentsTestData.agentWithFreespace(100000));
      agentC        = Agent.fromJSON(AgentsTestData.agentWithFreespace(20000));
      agentD        = Agent.fromJSON(AgentsTestData.agentWithFreespace("Unknown"));
      originalOrder = [agentA, agentB, agentC, agentD];
    });
    it("should sort agents in ascending order", () => {
      const comparator = new AgentComparator("freeSpace", SortOrder.ASC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentA, agentC, agentB, agentD]);
    });

    it("should sort in descending order", () => {
      const comparator = new AgentComparator("freeSpace", SortOrder.DESC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentD, agentB, agentC, agentA]);
    });
  });

  describe("Resource", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.agentWithResources(["dev", "fat"]));
      agentB        = Agent.fromJSON(AgentsTestData.agentWithResources(["firefox"]));
      agentC        = Agent.fromJSON(AgentsTestData.agentWithResources(["chrome"]));
      agentD        = Agent.fromJSON(AgentsTestData.agentWithResources([]));
      originalOrder = [agentA, agentB, agentC, agentD];
    });

    it("should sort in ascending order", () => {
      const comparator = new AgentComparator("resources", SortOrder.ASC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentD, agentC, agentA, agentB]);
    });

    it("should sort in descending order", () => {
      const comparator = new AgentComparator("resources", SortOrder.DESC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentB, agentA, agentC, agentD]);
    });
  });

  describe("Environments", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.agentWithEnvironments("prod", "dev", "qa"));
      agentB        = Agent.fromJSON(AgentsTestData.agentWithEnvironments("dev"));
      agentC        = Agent.fromJSON(AgentsTestData.agentWithEnvironments("qa"));
      agentD        = Agent.fromJSON(AgentsTestData.agentWithEnvironments());
      originalOrder = [agentA, agentB, agentC, agentD];
    });

    it("should sort in ascending order", () => {
      const comparator = new AgentComparator("environments", SortOrder.ASC);

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentD, agentB, agentA, agentC]);
    });

    it("should sort in descending order",
       () => {
         const comparator = new AgentComparator("environments", SortOrder.DESC);

         const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

         expect(sortedList).toEqual([agentC, agentA, agentB, agentD]);
       });
  });
});