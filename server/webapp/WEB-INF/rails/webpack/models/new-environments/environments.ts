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
import {Agents, EnvironmentAgentJSON} from "models/new-environments/environment_agents";
import {
  EnvironmentEnvironmentVariableJSON,
  EnvironmentVariables
} from "models/new-environments/environment_environment_variables";
import {EnvironmentPipelineJSON, Pipelines, PipelineWithOrigin} from "models/new-environments/environment_pipelines";
import {Origin, OriginJSON} from "models/new-environments/origin";

export interface EnvironmentJSON {
  name: string;
  origins: OriginJSON[];
  pipelines: EnvironmentPipelineJSON[];
  agents: EnvironmentAgentJSON[];
  environment_variables: EnvironmentEnvironmentVariableJSON[];
}

interface EmbeddedJSON {
  environments: EnvironmentJSON[];
}

export interface EnvironmentsJSON {
  _embedded: EmbeddedJSON;
}

export class EnvironmentWithOrigin {
  readonly name: Stream<string>;
  readonly origins: Stream<Origin[]>;
  readonly agents: Stream<Agents>;
  readonly pipelines: Stream<Pipelines>;
  readonly environmentVariables: Stream<EnvironmentVariables>;

  constructor(name: string,
              origins: Origin[],
              agents: Agents,
              pipelines: Pipelines,
              environmentVariables: EnvironmentVariables) {
    this.name                 = Stream(name);
    this.origins              = Stream(origins);
    this.agents               = Stream(agents);
    this.pipelines            = Stream(pipelines);
    this.environmentVariables = Stream(environmentVariables);
  }

  static fromJSON(data: EnvironmentJSON) {
    return new EnvironmentWithOrigin(data.name,
                                     data.origins.map((o) => Origin.fromJSON(o)),
                                     Agents.fromJSON(data.agents),
                                     Pipelines.fromJSON(data.pipelines),
                                     EnvironmentVariables.fromJSON(data.environment_variables));
  }

  containsPipeline(name: string): boolean {
    return this.pipelines().map((p) => p.name()).indexOf(name) !== -1;
  }

  addPipelineIfNotPresent(pipeline: PipelineWithOrigin) {
    if (!this.containsPipeline(pipeline.name())) {
      this.pipelines().push(pipeline);
    }
  }

  removePipelineIfPresent(pipeline: PipelineWithOrigin) {
    _.remove(this.pipelines(), (p) => p.name() === pipeline.name());
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
