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

import {Agent} from "models/new_agent/agents";
import _ from "lodash";

export enum SortOrder {
  ASC, DESC
}

export class AgentComparator {
  private readonly columnName: string;
  private readonly sortOrder: SortOrder;

  constructor(columnName: string, sortOrder: SortOrder) {
    this.columnName = columnName;
    this.sortOrder  = sortOrder;
  }

  compare(agentOne: Agent, agentTwo: Agent): number {
    const first = this.getColumnValue(agentOne);
    const other = this.getColumnValue(agentTwo);

    switch (this.columnName) {
      case "freeSpace":
        return this.compareFreespace(first, other);
      case "agentState":
        return this.compareAgentState(first, other);
      default:
        return this.compareString(first, other);
    }
  }

  private getColumnValue(agent: Agent) {
    switch (this.columnName) {
      case "environments":
        return agent.environmentNames().join(", ");
      case "agentState":
        return agent.status();
      default: {
        //@ts-ignore
        const value = agent[this.columnName];
        if (value instanceof Array) {
          return value.join(", ");
        }

        return `${value}`;
      }
    }
  }

  private compareAgentState(first: string, other: string) {
    const firstRank = AgentComparator.getAgentStatusRank(first);
    const otherRank = AgentComparator.getAgentStatusRank(other);
    return this.sortOrder === SortOrder.ASC ? firstRank - otherRank : otherRank - firstRank;
  }

  private compareFreespace(first: string, other: string) {
    const firstAsNumber = _.isNaN(parseInt(first)) ? Number.MAX_SAFE_INTEGER : parseInt(first);
    const otherAsNumber = _.isNaN(parseInt(other)) ? Number.MAX_SAFE_INTEGER : parseInt(other);
    return this.sortOrder === SortOrder.ASC ? firstAsNumber - otherAsNumber : otherAsNumber - firstAsNumber;
  }

  private compareString(first: string, other: string) {
    return this.sortOrder === SortOrder.ASC ? first.localeCompare(other) : other.localeCompare(first);
  }

  private static getAgentStatusRank(status: string): number {
    switch (status) {
      case "Pending":
        return 1;
      case "LostContact":
        return 2;
      case "Missing":
        return 3;
      case "Building":
        return 4;
      case "Building (Cancelled)":
        return 5;
      case "Idle":
        return 6;
      case "Disabled (Building)":
        return 7;
      case "Disabled (Cancelled)":
        return 8;
      case "Disabled":
        return 9;
      default:
        return Number.MAX_SAFE_INTEGER;
    }
  }
}