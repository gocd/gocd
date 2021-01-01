/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import Stream from "mithril/stream";
import {EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";

export interface PipelineJSON {
  name: string;
}

export interface EnvironmentJSON {
  name: string;
  agents: AgentJSON[];
  pipelines: PipelineJSON[];
  environment_variables: EnvironmentVariableJSON[];
}

interface EmbeddedJSON {
  environments: EnvironmentJSON[];
}

export interface EnvironmentsJSON {
  _embedded: EmbeddedJSON;
}

export class Pipeline {
  name: Stream<string>;

  constructor(name: string) {
    this.name = Stream(name);
  }

  static fromJSON(data: PipelineJSON) {
    return new Pipeline(data.name);
  }
}

class Pipelines extends Array<Pipeline> {
  constructor(...pipelines: Pipeline[]) {
    super(...pipelines);
    Object.setPrototypeOf(this, Object.create(Pipelines.prototype));
  }

  static fromJSON(pipelines: PipelineJSON[]) {
    return new Pipelines(...pipelines.map(Pipeline.fromJSON));
  }
}

class Environment {
  name: Stream<string>;
  pipelines: Stream<Pipelines>;
  environmentVariables: Stream<EnvironmentVariables>;

  constructor(name: string,
              pipelines: Pipelines,
              environmentVariables: EnvironmentVariables) {
    this.name                 = Stream(name);
    this.pipelines            = Stream(pipelines);
    this.environmentVariables = Stream(environmentVariables);
  }

  static fromJSON(data: EnvironmentJSON) {
    return new Environment(data.name,
                           Pipelines.fromJSON(data.pipelines),
                           EnvironmentVariables.fromJSON(data.environment_variables));
  }
}

export class Environments extends Array<Environment> {
  constructor(...environments: Environment[]) {
    super(...environments);
    Object.setPrototypeOf(this, Object.create(Environments.prototype));
  }

  static fromJSON(data: EnvironmentsJSON) {
    return new Environments(...data._embedded.environments.map(Environment.fromJSON));
  }
}

export interface AgentJSON {
  uuid: string;
}

export class Agent {
  uuid: Stream<string>;

  constructor(uuid: string) {
    this.uuid = Stream(uuid);
  }

  static fromJSON(data: AgentJSON) {
    return new Agent(data.uuid);
  }
}

export class Agents extends Array<Agent> {
  constructor(...agents: Agent[]) {
    super(...agents);
    Object.setPrototypeOf(this, Object.create(Agents.prototype));
  }

  static fromJSON(agents: AgentJSON[]) {
    return new Agents(...agents.map(Agent.fromJSON));
  }
}
