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
import {EnvironmentEnvironmentVariableJSON, EnvironmentVariablesWithOrigin} from "models/environment_variables/types";
import {PipelineJSON, Pipelines, PipelineWithOrigin} from "models/internal_pipeline_structure/pipeline_structure";
import {ErrorMessages} from "models/mixins/error_messages";
import {ValidatableMixin, Validator} from "models/mixins/new_validatable_mixin";
import {Agents, AgentWithOrigin, EnvironmentAgentJSON} from "models/new-environments/environment_agents";
import {EnvironmentsAPIs} from "models/new-environments/environments_apis";
import {Origin, OriginJSON, OriginType} from "models/origin";

export interface PermissionsJSON {
  can_edit: boolean;
  can_administer: boolean;
}

export interface EnvironmentJSON {
  name: string;
  permissions: PermissionsJSON;
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

class EnvironmentVariableNameUniquenessValidator extends Validator {
  protected doValidate(environmentWithOrigin: EnvironmentWithOrigin, attrName: string): void {
    _.forEach(environmentWithOrigin.environmentVariables(), (child) => {
      const duplicates = _.filter(environmentWithOrigin.environmentVariables(), (c) => {
        const isNameDuplicate   = c.name() === child.name();
        const isDifferentObject = c !== child;
        return (isNameDuplicate && isDifferentObject) && !_.isEmpty(c.name());
      });

      if (!_.isEmpty(duplicates)) {
        environmentWithOrigin.errors().add(attrName, ErrorMessages.duplicate("name"));
        child.errors().add("name", ErrorMessages.duplicate("name"));
      }

    });
  }
}

export class Permissions {
  readonly canEdit: Stream<boolean>;
  readonly canAdminister: Stream<boolean>;

  constructor(canEdit: boolean, canAdminister: boolean) {
    this.canEdit       = Stream(canEdit);
    this.canAdminister = Stream(canAdminister);
  }

  static fromJSON(json: PermissionsJSON) {
    return new Permissions(json.can_edit, json.can_administer);
  }
}

export class EnvironmentWithOrigin extends ValidatableMixin {
  readonly name: Stream<string>;
  readonly permissions: Stream<Permissions>;
  readonly origins: Stream<Origin[]>;
  readonly agents: Stream<Agents>;
  readonly pipelines: Stream<Pipelines>;
  readonly environmentVariables: Stream<EnvironmentVariablesWithOrigin>;

  constructor(name: string,
              permissions: Permissions,
              origins: Origin[],
              agents: Agents,
              pipelines: Pipelines,
              environmentVariables: EnvironmentVariablesWithOrigin) {
    super();
    ValidatableMixin.call(this);
    this.name                 = Stream(name);
    this.permissions          = Stream(permissions);
    this.origins              = Stream(origins);
    this.agents               = Stream(agents);
    this.pipelines            = Stream(pipelines);
    this.environmentVariables = Stream(environmentVariables);
    this.validatePresenceOf("name");
    this.validateEach("environmentVariables");
    this.validateWith(new EnvironmentVariableNameUniquenessValidator(), "environmentVariables");
  }

  static fromJSON(data: EnvironmentJSON) {
    let origins = [];
    if (data.origins) {
      origins = data.origins.map((o) => Origin.fromJSON(o));
    } else {
      origins.push(new Origin(OriginType.GoCD));
    }
    return new EnvironmentWithOrigin(data.name,
                                     Permissions.fromJSON(data.permissions),
                                     origins,
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
                                     new Permissions(true, true),
                                     this.origins().map((origin) => origin.clone()),
                                     this.agents().map((agent) => agent.clone()),
                                     this.pipelines().clone(),
                                     new EnvironmentVariablesWithOrigin(
                                       ...this.environmentVariables().map((p) => p.clone())
                                     ));
  }

  isLocal(): boolean {
    return this.origins().filter((origin) => origin.isDefinedInConfigRepo()).length === 0;
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
