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

import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";

interface PipelineJSON {
  name: string;
}

interface EnvironmentVariableJSON {
  secure: boolean;
  name: string;
  value: string;
  encrypted_value: string;
}

interface AgentJSON {
  uuid: string;
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

class Agent {
  uuid: Stream<string>;

  constructor(uuid: string) {
    this.uuid = stream(uuid);
  }

  static fromJSON(data: AgentJSON) {
    return new Agent(data.uuid);
  }
}

class Agents extends Array<Agent> {
  constructor(...agents: Agent[]) {
    super(...agents);
    Object.setPrototypeOf(this, Object.create(Agents.prototype));
  }

  static fromJSON(agents: AgentJSON[]) {
    return new Agents(...agents.map(Agent.fromJSON));
  }
}

class Pipeline {
  uuid: Stream<string>;

  constructor(uuid: string) {
    this.uuid = stream(uuid);
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

class EnvironmentVariable {
  secure: Stream<boolean>;
  name: Stream<string>;
  value: Stream<string>;
  encryptedValue: Stream<string>;

  constructor(name: string, value: string, secure: boolean, encryptedValue: string) {
    this.secure         = stream(secure);
    this.name           = stream(name);
    this.value          = stream(value);
    this.encryptedValue = stream(encryptedValue);
  }

  static fromJSON(data: EnvironmentVariableJSON) {
    return new EnvironmentVariable(data.name, data.value, data.secure, data.encrypted_value);
  }
}

class EnvironmentVariables extends Array<EnvironmentVariable> {
  constructor(...environmentVariables: EnvironmentVariable[]) {
    super(...environmentVariables);
    Object.setPrototypeOf(this, Object.create(EnvironmentVariables.prototype));
  }

  static fromJSON(environmentVariables: EnvironmentVariableJSON[]) {
    return new EnvironmentVariables(...environmentVariables.map(EnvironmentVariable.fromJSON));
  }
}

class Environment {
  name: Stream<string>;
  agents: Stream<Agents>;
  pipelines: Stream<Pipelines>;
  environmentVariables: Stream<EnvironmentVariables>;

  constructor(name: string,
              agents: Agents,
              pipelines: Pipelines,
              environmentVariables: EnvironmentVariables) {
    this.name                 = stream(name);
    this.agents               = stream(agents);
    this.pipelines            = stream(pipelines);
    this.environmentVariables = stream(environmentVariables);
  }

  static fromJSON(data: EnvironmentJSON) {
    return new Environment(data.name,
                           Agents.fromJSON(data.agents),
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
