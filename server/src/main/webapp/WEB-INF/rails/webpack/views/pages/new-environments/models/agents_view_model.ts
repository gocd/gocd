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

import _ from "lodash";
import Stream from "mithril/stream";
import {Agent, Agents} from "models/agents/agents";
import {AgentWithOrigin} from "models/new-environments/environment_agents";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import {Origin, OriginType} from "models/origin";

export class AgentsViewModel {
  readonly searchText: Stream<string | undefined>;
  readonly agents: Stream<Agents>;
  readonly environment: EnvironmentWithOrigin;
  readonly selectedAgentUuids: string[];
  readonly removedAgentUuids: string[];

  constructor(environment: EnvironmentWithOrigin, agents: Agents) {
    this.environment        = environment;
    this.searchText         = Stream();
    this.agents             = Stream(agents);
    this.selectedAgentUuids = [];
    environment.agents().map((agent) => {
      if (agents.hasAgent(agent.uuid())) {
        this.selectedAgentUuids.push(agent.uuid());
      }
    });
    this.removedAgentUuids = [];
  }

  filteredAgents() {
    const searchText = this.searchText() ? this.searchText()! : "";

    return new Agents(...this.agents()!.filter(((agent) => {
      return agent.hostname.includes(searchText);
    })));
  }

  availableAgents(): Agents {
    return new Agents(...this.filteredAgents().filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      const belongsToEnvironment: boolean      = !!found;

      if (belongsToEnvironment) {
        return !found!.origin().isDefinedInConfigRepo() && !agent.isElastic();
      }

      return !agent.isElastic();
    }));
  }

  configRepoEnvironmentAgents(): Agents {
    return new Agents(...this.filteredAgents().filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      return found && found.origin().isDefinedInConfigRepo();
    }));
  }

  environmentElasticAgents(): Agents {
    return new Agents(...this.filteredAgents().filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      return found && agent.isElastic();
    }));
  }

  elasticAgentsNotBelongingToCurrentEnv(): Agents {
    const self = this;
    return new Agents(...this.filteredAgents().filter((agent) => {
      return agent.isElastic() && !self.environment.containsAgent(agent.uuid);
    }));
  }

  agentSelectedFn(selected: Agent): (value?: any) => any {
    const agent = new AgentWithOrigin(selected.uuid, selected.hostname, new Origin(OriginType.GoCD));

    return (value?: boolean) => {
      if (value !== undefined) {
        if (value) {
          this.selectedAgentUuids.push(agent.uuid());
        } else {
          _.remove(this.selectedAgentUuids, (uuid) => uuid === agent.uuid());
          if (this.environment.agents().find((envAgent) => envAgent.uuid() === agent.uuid())) {
            this.removedAgentUuids.push(agent.uuid());
          }
        }
      }
      return this.selectedAgentUuids.includes(selected.uuid);
    };
  }
}
