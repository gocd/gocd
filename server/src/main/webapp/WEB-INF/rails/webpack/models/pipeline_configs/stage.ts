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

import {JsonUtils} from "helpers/json_utils";
import Stream from "mithril/stream";
import {AuthorizationUsersAndRolesJSON, AuthorizedUsersAndRoles} from "models/authorization/authorization";
import {EnvironmentVariableJSON, EnvironmentVariables} from "models/environment_variables/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Job, JobJSON} from "./job";
import {NameableSet} from "./nameable_set";

export interface ApprovalJSON {
  type: ApprovalType;
  allow_only_on_success: boolean;
  authorization: AuthorizationUsersAndRolesJSON;
}

export interface StageJSON {
  name: string;
  fetch_materials: boolean;
  clean_working_directory: boolean;
  never_cleanup_artifacts: boolean;
  approval: ApprovalJSON;
  environment_variables: EnvironmentVariableJSON[];
  jobs: JobJSON[];
}

export type ApprovalType = "success" | "manual";

class Approval extends ValidatableMixin {
  readonly type = Stream<ApprovalType>("success");

  readonly state: (value?: boolean) => boolean;
  readonly allowOnlyOnSuccess = Stream<boolean>();
  //authorization must be present for server side validations
  //even though it's not editable from the create pipeline page
  readonly authorization          = Stream<AuthorizedUsersAndRoles>(new AuthorizedUsersAndRoles([], []));
  private readonly __typeAsStream = Stream<boolean>(true);

  constructor() {
    super();

    this.validatePresenceOf("type");
    this.validatePresenceOf("authorization");

    // define like this because it will be bound later to the component
    // and `this` won't refer to an Approval object
    this.state = (value?: boolean) => {
      if ("boolean" === typeof value) {
        this.type(value ? "success" : "manual");
        this.__typeAsStream(this.type() === "success");
      }

      return "success" === this.type();
    };
  }

  static fromJSON(json: ApprovalJSON) {
    const approval = new Approval();
    if (!json) {
      return approval;
    }

    approval.type(json.type || "success");
    approval.__typeAsStream(json.type === "success");

    approval.authorization(AuthorizedUsersAndRoles.fromJSON(json.authorization));
    approval.allowOnlyOnSuccess(json.allow_only_on_success);
    return approval;
  }

  typeAsString() {
    return this.__typeAsStream() ? "success" : "manual";
  }

  typeAsStream() {
    return this.__typeAsStream;
  }

  toJSON() {
    return {
      type: this.typeAsString(),
      allow_only_on_success: this.allowOnlyOnSuccess(),
      authorization: JsonUtils.toSnakeCasedObject(this.authorization)
    };
  }
}

export class Stage extends ValidatableMixin {
  readonly name                  = Stream<string>();
  readonly approval              = Stream(new Approval());
  readonly jobs                  = Stream<NameableSet<Job>>();
  readonly fetchMaterials        = Stream<boolean>();
  readonly cleanWorkingDirectory = Stream<boolean>();
  readonly neverCleanupArtifacts = Stream<boolean>();
  readonly environmentVariables  = Stream<EnvironmentVariables>();

  private readonly __name = Stream<string>();

  constructor(name: string = "", jobs: Job[] = []) {
    super();

    this.__name = Stream(name);

    this.name = Stream(name);
    this.validatePresenceOf("name");
    this.validateIdFormat("name");

    this.validateAssociated("approval");

    this.jobs = Stream(new NameableSet(jobs));
    this.validateNonEmptyCollection("jobs", {message: `A stage must have at least one job`});
    this.validateAssociated("jobs");
  }

  static fromJSONArray(stages: StageJSON[]) {
    return stages.map(this.fromJSON);
  }

  static fromJSON(json: StageJSON) {
    const stage = new Stage();
    stage.name(json.name);
    stage.__name(json.name);
    stage.fetchMaterials(json.fetch_materials);
    stage.cleanWorkingDirectory(json.clean_working_directory);
    stage.neverCleanupArtifacts(json.never_cleanup_artifacts);
    stage.approval(Approval.fromJSON(json.approval));
    stage.environmentVariables(EnvironmentVariables.fromJSON(json.environment_variables || []));
    stage.jobs(new NameableSet(Job.fromJSONArray(json.jobs || [])));
    return stage;
  }

  firstJob(): Job {
    return this.jobs().values().next().value;
  }

  getOriginalName() {
    return this.__name();
  }

  toApiPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }
}
