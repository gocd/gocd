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

import {JsonUtils} from "helpers/json_utils";
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {EnvironmentVariableConfig} from "models/new_pipeline_configs/environment_variable_config";

enum ApprovalType { success = "success", manual = "manual" }

export interface StringListInterface {
  type(): string;

  add(element: string): void;

  remove(element: string): void;

  list(): string[];
}

abstract class StringList implements StringListInterface {
  private readonly internalList: Stream<string[]>;

  constructor(list?: string[]) {
    this.internalList = Stream(list || []);
  }

  add(element: string): void {
    if (!this.exists(element)) {
      this.internalList().push(element);
    }
  }

  remove(element: string) {
    if (this.exists(element)) {
      this.internalList(this.internalList().filter((user) => user !== element));
    }
  }

  list() {
    return this.internalList();
  }

  abstract type(): string;

  private exists(element: string): boolean {
    return this.internalList().indexOf(element) !== -1;
  }
}

class Users extends StringList {
  type(): string {
    return "User";
  }
}

class Roles extends StringList {
  type(): string {
    return "Role";
  }
}

export class StageAuthorization extends ValidatableMixin {
  public readonly users: Users;
  public readonly roles: Roles;

  constructor(users?: string[], roles?: string[]) {
    super();

    this.users = new Users(users);
    this.roles = new Roles(roles);

    this.validatePresenceOf("users");
    this.validatePresenceOf("roles");
  }
}

class Approval extends ValidatableMixin {
  public readonly type: Stream<ApprovalType> = Stream();
  public readonly state: (value?: boolean) => boolean;
  public readonly inheritFromPipelineGroup: (value?: boolean) => boolean;
  public readonly authorization: Stream<StageAuthorization | undefined>;

  constructor(authorization?: StageAuthorization) {
    super();

    this.type(ApprovalType.success);
    this.authorization = Stream(authorization);

    this.validatePresenceOf("type");

    this.state = (value?: boolean) => {
      if ("boolean" === typeof value) {
        this.type(value ? ApprovalType.success : ApprovalType.manual);
      }
      return ApprovalType.success === this.type();
    };

    this.inheritFromPipelineGroup = (value?: boolean) => {
      if ("boolean" === typeof value) {
        value ? this.authorization(undefined) : this.authorization(new StageAuthorization());
      }

      return !this.authorization();
    };
  }
}

export class StageConfig extends ValidatableMixin {
  readonly name: Stream<string>;
  readonly approval: Stream<Approval>;
  readonly fetchMaterials: Stream<boolean>;
  readonly cleanWorkingDirectory: Stream<boolean>;
  readonly neverCleanupArtifacts: Stream<boolean>;
  readonly environmentVariables: Stream<EnvironmentVariableConfig[]>;

  constructor(name: string,
              approval?: Approval,
              fetchMaterials?: boolean,
              cleanWorkingDirectory?: boolean,
              neverCleanupArtifacts?: boolean,
              environmentVariables?: EnvironmentVariableConfig[]) {
    super();

    this.name                  = Stream(name);
    this.approval              = approval ? Stream(approval) : Stream(new Approval());
    this.fetchMaterials        = Stream(!!fetchMaterials);
    this.cleanWorkingDirectory = Stream(!!cleanWorkingDirectory);
    this.neverCleanupArtifacts = Stream(!!neverCleanupArtifacts);
    this.environmentVariables  = Stream(environmentVariables || []);

    this.validatePresenceOf("name");
    this.validatePresenceOf("fetchMaterials");
    this.validatePresenceOf("cleanWorkingDirectory");
    this.validatePresenceOf("neverCleanupArtifacts");

    this.validateIdFormat("name");

    this.validateAssociated("approval");
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
