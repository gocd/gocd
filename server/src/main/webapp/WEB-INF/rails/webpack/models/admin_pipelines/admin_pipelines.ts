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
import {Authorization, AuthorizationJSON, AuthorizedUsersAndRoles, PermissionForEntity, PermissionsForUsersAndRoles} from "../authorization/authorization";

export interface PipelineGroupJSON {
  name: string;
  authorization: AuthorizationJSON;
}

export class PipelineGroup {
  readonly name: Stream<string>;
  readonly authorization: Stream<Authorization>;

  constructor(name: string, authorization: Authorization) {
    this.name          = Stream(name);
    this.authorization = Stream(authorization);
  }

  static fromJSON(data: PipelineGroupJSON) {
    return new PipelineGroup(data.name, Authorization.fromJSON(data.authorization));
  }

  toJSON() {
    return {
      name: this.name(),
      authorization: this.authorization().toJSON()
    };
  }
}

export class PipelineGroupViewModel {
  readonly name: Stream<string>;
  private readonly authorizationViewModel: Stream<PermissionsForUsersAndRoles>;

  constructor(pipelineGroup: PipelineGroup) {
    this.name                   = Stream(pipelineGroup.name());
    this.authorizationViewModel = Stream(new PermissionsForUsersAndRoles(pipelineGroup.authorization()));
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

  getUpdatedPipelineGroup(): PipelineGroup {
    const viewAccess    = new AuthorizedUsersAndRoles(
      this.authorizedUsers().filter((user) => !_.isEmpty(user.name()) && user.view()).map((user) => Stream(user.name())),
      this.authorizedRoles().filter((role) => !_.isEmpty(role.name()) && role.view()).map((role) => Stream(role.name()))
    );
    const operateAccess = new AuthorizedUsersAndRoles(
      this.authorizedUsers().filter((user) => !_.isEmpty(user.name()) && user.operate()).map((user) => Stream(user.name())),
      this.authorizedRoles().filter((role) => !_.isEmpty(role.name()) && role.operate()).map((role) => Stream(role.name()))
    );
    const adminAccess   = new AuthorizedUsersAndRoles(
      this.authorizedUsers().filter((user) => !_.isEmpty(user.name()) && user.admin()).map((user) => Stream(user.name())),
      this.authorizedRoles().filter((role) => !_.isEmpty(role.name()) && role.admin()).map((role) => Stream(role.name()))
    );

    return new PipelineGroup(this.name(), new Authorization(viewAccess, adminAccess, operateAccess));
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
