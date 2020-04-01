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

import {Agent} from "models/agents/agents";
import {AgentComparator} from "models/agents/agent_comparator";
import {AgentsTestData} from "models/agents/spec/agents_test_data";

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
      const comparator = new AgentComparator("agentState");

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      const expectedAfterSort = [pending, lostContact, missing, building, buildingCancelled, idle, disabledBuilding, disabledCanceled, disabled];
      expect(sortedList).toEqual(expectedAfterSort);
    });
  });

  describe("Free Space", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.withFreespace(10000));
      agentB        = Agent.fromJSON(AgentsTestData.withFreespace(100000));
      agentC        = Agent.fromJSON(AgentsTestData.withFreespace(20000));
      agentD        = Agent.fromJSON(AgentsTestData.withFreespace("Unknown"));
      originalOrder = [agentA, agentB, agentC, agentD];
    });
    it("should sort agents in ascending order", () => {
      const comparator = new AgentComparator("freeSpace");

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentA, agentC, agentB, agentD]);
    });
  });

  describe("Resource", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.withResources("dev", "fat"));
      agentB        = Agent.fromJSON(AgentsTestData.withResources("firefox"));
      agentC        = Agent.fromJSON(AgentsTestData.withResources("chrome"));
      agentD        = Agent.fromJSON(AgentsTestData.withResources());
      originalOrder = [agentA, agentB, agentC, agentD];
    });

    it("should sort in ascending order", () => {
      const comparator = new AgentComparator("resources");

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentD, agentC, agentA, agentB]);
    });

  });

  describe("Environments", () => {
    let agentA: Agent, agentB: Agent, agentC: Agent, agentD: Agent;
    beforeEach(() => {
      agentA        = Agent.fromJSON(AgentsTestData.withEnvironments("prod", "dev", "qa"));
      agentB        = Agent.fromJSON(AgentsTestData.withEnvironments("dev"));
      agentC        = Agent.fromJSON(AgentsTestData.withEnvironments("qa"));
      agentD        = Agent.fromJSON(AgentsTestData.withEnvironments());
      originalOrder = [agentA, agentB, agentC, agentD];
    });

    it("should sort in ascending order", () => {
      const comparator = new AgentComparator("environments");

      const sortedList = originalOrder.sort(comparator.compare.bind(comparator));

      expect(sortedList).toEqual([agentD, agentB, agentA, agentC]);
    });

  });
});
