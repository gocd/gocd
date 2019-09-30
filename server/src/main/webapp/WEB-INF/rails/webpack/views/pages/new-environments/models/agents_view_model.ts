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

import m from "mithril";
import Stream from "mithril/stream";
import {AgentWithOrigin} from "models/new-environments/environment_agents";
import {Environments, EnvironmentWithOrigin} from "models/new-environments/environments";
import {Origin, OriginType} from "models/new-environments/origin";
import {Agent, Agents} from "models/new_agent/agents";
import {AgentsCRUD} from "models/new_agent/agents_crud";

export class AgentsViewModel {
  readonly searchText: Stream<string | undefined>;
  readonly errorMessage: Stream<string | undefined>;
  readonly agents: Stream<Agents | undefined>;
  readonly environment: EnvironmentWithOrigin;
  readonly environments: Environments;

  constructor(environment: EnvironmentWithOrigin, environments: Environments) {
    this.environment  = environment;
    this.environments = environments;
    this.searchText   = Stream();
    this.errorMessage = Stream();
    this.agents       = Stream();
  }

  availableAgents(): Agents {
    return new Agents(...this.agents()!.filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      const belongsToEnvironment: boolean      = !!found;

      if (belongsToEnvironment) {
        return !found!.origin().isDefinedInConfigRepo() && !agent.isElastic();
      }

      return !agent.isElastic();
    }));
  }

  configRepoEnvironmentAgents(): Agents {
    return new Agents(...this.agents()!.filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      return found && found.origin().isDefinedInConfigRepo();
    }));
  }

  environmentElasticAgents(): Agents {
    return new Agents(...this.agents()!.filter((agent) => {
      const found: AgentWithOrigin | undefined = this.environment.agents().find((a) => a.uuid() === agent.uuid);
      return found && agent.isElastic();
    }));
  }

  elasticAgentsNotBelongingToCurrentEnv(): Agents {
    const self = this;
    return new Agents(...this.agents()!.filter((agent) => {
      return agent.isElastic() && !self.environment.containsAgent(agent.uuid);
    }));
  }

  agentSelectedFn(selected: Agent): (value?: any) => any {
    const self  = this;
    const agent = new AgentWithOrigin(selected.uuid, new Origin(OriginType.GoCD));

    return (value?: boolean) => {
      if (value !== undefined) {
        value ? self.environment.addAgentIfNotPresent(agent) : self.environment.removeAgentIfPresent(agent);
      }
      return self.environment.containsAgent(selected.uuid);
    };
  }

  fetchAllAgents(callback: () => void) {
    AgentsCRUD.all().then((result) =>
                            result.do((successResponse) => {
                              this.agents(successResponse.body);
                              callback();
                            }, (errorResponse) => {
                              this.errorMessage(JSON.parse(errorResponse.body!).message);
                            })).finally(m.redraw);
  }
}
