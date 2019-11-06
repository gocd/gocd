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

import _ from "lodash";
import Stream from "mithril/stream";
import {PipelineJSON, Pipelines, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Agents, AgentWithOrigin, EnvironmentAgentJSON} from "models/new-environments/environment_agents";
import {
  EnvironmentEnvironmentVariableJSON,
  EnvironmentVariablesWithOrigin
} from "models/new-environments/environment_environment_variables";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Origin, OriginJSON} from "models/origin";

export interface EnvironmentJSON {
  name: string;
  origins: OriginJSON[];
  pipelines: PipelineJSON[];
  agents: EnvironmentAgentJSON[];
  environment_variables: EnvironmentEnvironmentVariableJSON[];
}

interface EmbeddedJSON {
  environments: EnvironmentJSON[];
}

export interface EnvironmentsJSON {
  _embedded: EmbeddedJSON;
}

export class EnvironmentWithOrigin extends ValidatableMixin {
  readonly name: Stream<string>;
  readonly origins: Stream<Origin[]>;
  readonly agents: Stream<Agents>;
  readonly pipelines: Stream<Pipelines>;
  readonly environmentVariables: Stream<EnvironmentVariablesWithOrigin>;

  constructor(name: string,
              origins: Origin[],
              agents: Agents,
              pipelines: Pipelines,
              environmentVariables: EnvironmentVariablesWithOrigin) {
    super();
    ValidatableMixin.call(this);
    this.name                 = Stream(name);
    this.origins              = Stream(origins);
    this.agents               = Stream(agents);
    this.pipelines            = Stream(pipelines);
    this.environmentVariables = Stream(environmentVariables);
    this.validatePresenceOf("name");
    this.validateAssociated("environmentVariables");
    this.validateChildAttrIsUnique("environmentVariables", "name");
  }

  static fromJSON(data: EnvironmentJSON) {
    return new EnvironmentWithOrigin(data.name,
                                     data.origins.map((o) => Origin.fromJSON(o)),
                                     Agents.fromJSON(data.agents),
                                     Pipelines.fromJSON(data.pipelines),
                                     EnvironmentVariablesWithOrigin.fromJSON(data.environment_variables));
  }

  containsPipeline(name: string): boolean {
    return this.pipelines().map((p) => p.name()).indexOf(name) !== -1;
  }

  containsAgent(uuid: string): boolean {
    return this.agents().map((a) => a.uuid()).indexOf(uuid) !== -1;
  }

  addPipelineIfNotPresent(pipeline: PipelineWithOrigin) {
    if (!this.containsPipeline(pipeline.name())) {
      this.pipelines().push(pipeline);
    }
  }

  addAgentIfNotPresent(agent: AgentWithOrigin) {
    if (!this.containsAgent(agent.uuid())) {
      this.agents().push(agent);
    }
  }

  delete() {
    return EnvironmentsAPIs.delete(this.name());
  }

  removePipelineIfPresent(pipeline: PipelineWithOrigin) {
    _.remove(this.pipelines(), (p) => p.name() === pipeline.name());
  }

  removeAgentIfPresent(agent: AgentWithOrigin) {
    _.remove(this.agents(), (p) => p.uuid() === agent.uuid());
  }

  toJSON(): object {
    return {
      name: this.name(),
      environment_variables: this.environmentVariables().toJSON()
    };
  }

  clone(): EnvironmentWithOrigin {
    return new EnvironmentWithOrigin(this.name(),
                                     this.origins().map((origin) => origin.clone()),
                                     this.agents().map((agent) => agent.clone()),
                                     this.pipelines().clone(),
                                     new EnvironmentVariablesWithOrigin(
                                       ...this.environmentVariables().map((p) => p.clone())
                                     ));
  }
}

export class Environments extends Array<EnvironmentWithOrigin> {
  constructor(...environments: EnvironmentWithOrigin[]) {
    super(...environments);
    Object.setPrototypeOf(this, Object.create(Environments.prototype));
  }

  static fromJSON(data: EnvironmentsJSON) {
    return new Environments(...data._embedded.environments.map(EnvironmentWithOrigin.fromJSON));
  }

  findEnvironmentForPipeline(pipelineName: string): EnvironmentWithOrigin | undefined {
    for (const env of this) {
      if (env.containsPipeline(pipelineName)) {
        return env;
      }
    }
  }

  isPipelineDefinedInAnotherEnvironmentApartFrom(envName: string, pipelineName: string): boolean {
    return this.some((env) => {
      if (env.name() === envName) {
        return false;
      }

      return env.containsPipeline(pipelineName);
    });
  }
}
