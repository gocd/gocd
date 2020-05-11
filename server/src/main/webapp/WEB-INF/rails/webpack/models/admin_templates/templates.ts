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
import {
  Authorization,
  AuthorizationUsersAndRolesJSON,
  AuthorizedUsersAndRoles,
  PermissionForEntity,
  PermissionsForUsersAndRoles
} from "models/authorization/authorization";
import {StageJSON} from "models/pipeline_configs/stage";
import {RunIfCondition} from "models/pipeline_configs/task";
import {PropertyJSON} from "models/shared/configuration";

export namespace TemplateSummary {

  export interface TemplateSummaryRootObject {
    _embedded: { templates: TemplateSummaryTemplate[] };
  }

  export interface TemplateSummaryTemplate {
    name: string;
    can_edit: boolean;
    can_administer: boolean;
    _embedded: { pipelines: TemplateSummaryPipeline[] };
  }

  export interface TemplateSummaryPipeline {
    name: string;
    can_administer: boolean;
  }
}

export interface Template {
  name: string;
  stages: StageJSON[];
}

export type TaskType = "pluggable_task" | "fetch" | "ant" | "exec" | "nant" | "rake";

export interface TaskJSON {
  type: TaskType;
  attributes: AntTaskAttributesJSON | NAntTaskAttributesJSON | RakeTaskAttributesJSON | ExecTaskAttributesJSON | FetchTaskAttributesJSON | PluginTaskAttributesJSON;
}

export interface BaseTaskAttributesJSON {
  run_if: (RunIfCondition)[];
  on_cancel?: TaskJSON;
}

export interface WorkingDirAttributesJSON {
  working_directory?: string;
}

export interface BuildFileAndTargetBasedTaskAttributes extends WorkingDirAttributesJSON {
  build_file?: string;
  target?: string;
}

export type AntTaskAttributesJSON = BaseTaskAttributesJSON & BuildFileAndTargetBasedTaskAttributes ;

export interface NAntTaskAttributesJSON extends BaseTaskAttributesJSON, BuildFileAndTargetBasedTaskAttributes {
  nant_path?: string;
}

export type RakeTaskAttributesJSON = BaseTaskAttributesJSON & BuildFileAndTargetBasedTaskAttributes ;

export interface ExecTaskAttributesJSON extends BaseTaskAttributesJSON, WorkingDirAttributesJSON {
  command: string;

  // either of these can be present
  args?: string;
  arguments?: string[];
}

export interface PluginConfiguration {
  id: string;
  version: string;
}

export type ArtifactOrigin = "external" | "gocd";

export interface FetchTaskAttributesJSON extends BaseTaskAttributesJSON {
  artifact_origin: ArtifactOrigin;
  pipeline?: string;
  stage: string;
  job: string;

  // for regular fetch
  is_source_a_file?: boolean;
  source?: string;
  destination?: string;

  // for external fetch
  artifact_id?: string;
  configuration?: PropertyJSON[];
}

export interface PluginTaskAttributesJSON extends BaseTaskAttributesJSON {
  plugin_configuration: PluginConfiguration;
  configuration: PropertyJSON[];
}

export interface TemplateAuthorizationJSON {
  all_group_admins_are_view_users: boolean;
  admin?: AuthorizationUsersAndRolesJSON;
  view?: AuthorizationUsersAndRolesJSON;
}

export class TemplateAuthorization {
  allGroupAdminsAreViewUsers: Stream<boolean>;
  admin: Stream<AuthorizedUsersAndRoles | undefined>;
  view: Stream<AuthorizedUsersAndRoles | undefined>;

  constructor(allGroupAdminsAreViewUsers: boolean, admin?: AuthorizedUsersAndRoles, view?: AuthorizedUsersAndRoles) {
    this.allGroupAdminsAreViewUsers = Stream(allGroupAdminsAreViewUsers);
    this.admin                      = Stream(admin);
    this.view                       = Stream(view);
  }

  static fromJSON(data: TemplateAuthorizationJSON): TemplateAuthorization {
    return new TemplateAuthorization(data.all_group_admins_are_view_users,
                                     data.admin ? AuthorizedUsersAndRoles.fromJSON(data.admin) : undefined,
                                     data.view ? AuthorizedUsersAndRoles.fromJSON(data.view) : undefined);
  }

  toJSON() {
    const json: TemplateAuthorizationJSON = {
      all_group_admins_are_view_users: this.allGroupAdminsAreViewUsers(),
    };
    if (this.view() !== undefined && !this.view()!.isEmpty()) {
      json.view = {
        users: this.view()!.users(),
        roles: this.view()!.roles()
      };
    }
    if (this.admin() !== undefined && !this.admin()!.isEmpty()) {
      json.admin = {
        users: this.admin()!.users(),
        roles: this.admin()!.roles()
      };
    }
    return json;
  }
}

export class TemplateAuthorizationViewModel {
  allGroupAdminsAreViewUsers: Stream<boolean>;
  private readonly authorizationViewModel: Stream<PermissionsForUsersAndRoles>;

  constructor(tempAuth: TemplateAuthorization) {
    this.allGroupAdminsAreViewUsers = tempAuth.allGroupAdminsAreViewUsers;
    this.authorizationViewModel     = Stream(new PermissionsForUsersAndRoles(new Authorization(tempAuth.view(), tempAuth.admin())));
  }

  authorizedUsers() {
    return this.authorizationViewModel().authorizedUsers();
  }

  authorizedRoles() {
    return this.authorizationViewModel().authorizedRoles();
  }

  addAuthorizedUser(authorizedEntity: PermissionForEntity) {
    this.authorizationViewModel().addAuthorizedUser(authorizedEntity);
  }

  addAuthorizedRole(authorizedEntity: PermissionForEntity) {
    this.authorizationViewModel().addAuthorizedRole(authorizedEntity);
  }

  removeAuthorizedRole(role: PermissionForEntity) {
    this.authorizationViewModel().removeRole(role);
  }

  removeAuthorizedUser(user: PermissionForEntity) {
    this.authorizationViewModel().removeUser(user);
  }

  getUpdatedTemplateAuthorization(): TemplateAuthorization {
    const adminAccess = new AuthorizedUsersAndRoles(
      this.authorizedUsers().filter((user) => !_.isEmpty(user.name()) && user.admin()).map((user) => Stream(user.name())),
      this.authorizedRoles().filter((role) => !_.isEmpty(role.name()) && role.admin()).map((role) => Stream(role.name()))
    );
    const viewAccess  = new AuthorizedUsersAndRoles(
      this.authorizedUsers().filter((user) => !_.isEmpty(user.name()) && user.view() && !user.admin()).map((user) => Stream(user.name())),
      this.authorizedRoles().filter((role) => !_.isEmpty(role.name()) && role.view() && !role.admin()).map((role) => Stream(role.name()))
    );

    return new TemplateAuthorization(this.allGroupAdminsAreViewUsers(), adminAccess, viewAccess);
  }

  isValid() {
    return this.authorizationViewModel().isValid();
  }

  errorsOnRoles(): string[] {
    return this.authorizationViewModel().errors().errors('roles') as string[];
  }

  errorsOnUsers(): string[] {
    return this.authorizationViewModel().errors().errors('users') as string[];
  }
}
