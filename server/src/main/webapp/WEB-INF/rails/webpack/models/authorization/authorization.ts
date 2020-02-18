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
import {Errors} from "../mixins/errors";
import {ValidatableMixin, Validator} from "../mixins/new_validatable_mixin";

export interface AuthorizationUsersAndRolesJSON {
  users: string[];
  roles: string[];
  errors?: { [key: string]: string[] };
}

export interface AuthorizationJSON {
  view?: AuthorizationUsersAndRolesJSON;
  admins?: AuthorizationUsersAndRolesJSON;
  operate?: AuthorizationUsersAndRolesJSON;
}

export class AuthorizedUsersAndRoles extends ValidatableMixin {
  readonly users: Stream<string[]>;
  readonly roles: Stream<string[]>;

  constructor(users: string[], roles: string[], errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.users = Stream(users);
    this.roles = Stream(roles);
    this.errors(errors);
  }

  static fromJSON(usersAndRoles: AuthorizationUsersAndRolesJSON) {
    const errors = new Errors(usersAndRoles.errors);
    return new AuthorizedUsersAndRoles(usersAndRoles.users, usersAndRoles.roles, errors);
  }

  isEmpty(): boolean {
    return _.isEmpty(this.users()) && _.isEmpty(this.roles());
  }

  toJSON(): AuthorizationUsersAndRolesJSON {
    return {
      users: this.users(),
      roles: this.roles()
    };
  }
}

export class Authorization {
  readonly view: Stream<AuthorizedUsersAndRoles>;
  readonly admin: Stream<AuthorizedUsersAndRoles>;
  readonly operate: Stream<AuthorizedUsersAndRoles>;

  constructor(view?: AuthorizedUsersAndRoles, admin?: AuthorizedUsersAndRoles, operate?: AuthorizedUsersAndRoles) {
    this.view    = Stream(view ? view : new AuthorizedUsersAndRoles([], []));
    this.admin   = Stream(admin ? admin : new AuthorizedUsersAndRoles([], []));
    this.operate = Stream(operate ? operate : new AuthorizedUsersAndRoles([], []));
  }

  static fromJSON(authorization: AuthorizationJSON) {
    return new Authorization(
      authorization.view ? AuthorizedUsersAndRoles.fromJSON(authorization.view) : undefined,
      authorization.admins ? AuthorizedUsersAndRoles.fromJSON(authorization.admins) : undefined,
      authorization.operate ? AuthorizedUsersAndRoles.fromJSON(authorization.operate) : undefined);
  }

  toJSON(): AuthorizationJSON {
    const json: AuthorizationJSON = {};

    if (!this.view().isEmpty()) {
      json.view = this.view().toJSON();
    }
    if (!this.operate().isEmpty()) {
      json.operate = this.operate().toJSON();
    }
    if (!this.admin().isEmpty()) {
      json.admins = this.admin().toJSON();
    }

    return json;
  }
}

class PermissionValidator extends Validator {
  protected doValidate(entity: PermissionForEntity, attrName: string): void {
    if (entity.view() === false && entity.operate() === false && entity.admin() === false) {
      entity.errors().add(attrName, "At least one permission should be enabled.");
    }
  }
}

export class PermissionForEntity extends ValidatableMixin {
  readonly name: Stream<string>;
  readonly view: Stream<boolean>;
  readonly operate: Stream<boolean>;
  readonly admin: Stream<boolean>;

  constructor(name: string, view: boolean, operate: boolean, admin: boolean) {
    super();
    this.name    = Stream(name);
    this.view    = Stream(view);
    this.operate = Stream(operate);
    this.admin   = Stream(admin);

    this.setPermissions();
    ValidatableMixin.call(this);

    this.validateWith(new PermissionValidator({condition: () => !_.isEmpty(this.name())}), "name");
  }

  setPermissions() {
    if (this.admin()) {
      this.view(true);
      this.operate(true);
    } else if (this.operate()) {
      this.view(true);
    }
  }
}

export class PermissionsForUsersAndRoles extends ValidatableMixin {
  readonly authorizedUsers: Stream<PermissionForEntity[]> = Stream();
  readonly authorizedRoles: Stream<PermissionForEntity[]> = Stream();

  constructor(authorization: Authorization) {
    super();
    ValidatableMixin.call(this);

    this.initializeAuthorizedUsers(authorization);
    this.initializeAuthorizedRoles(authorization);
    this.initializeErrors(authorization);

    this.validateEach("authorizedUsers");
    this.validateEach("authorizedRoles");
  }

  addAuthorizedUser(authorizedEntity: PermissionForEntity) {
    this.authorizedUsers().push(authorizedEntity);
  }

  addAuthorizedRole(authorizedEntity: PermissionForEntity) {
    this.authorizedRoles().push(authorizedEntity);
  }

  removeRole(role: PermissionForEntity) {
    const index = this.authorizedRoles().indexOf(role);
    this.authorizedRoles().splice(index, 1);
  }

  removeUser(user: PermissionForEntity) {
    const index = this.authorizedUsers().indexOf(user);
    this.authorizedUsers().splice(index, 1);
  }

  private initializeAuthorizedUsers(authorization: Authorization) {
    let users = _.concat(authorization.view().users(), authorization.operate().users(), authorization.admin().users());
    users     = _.uniq(users);

    this.authorizedUsers(
      users.map((user: string) => new PermissionForEntity(user,
                                                          _.includes(authorization.view().users(), user),
                                                          _.includes(authorization.operate().users(), user),
                                                          _.includes(authorization.admin().users(), user)
                )
      )
    );
  }

  private initializeAuthorizedRoles(authorization: Authorization) {
    let roles = _.concat(authorization.view().roles(), authorization.operate().roles(), authorization.admin().roles());
    roles     = _.uniq(roles);
    this.authorizedRoles(roles.map((role: string) => new PermissionForEntity(role,
                                                                             _.includes(authorization.view().roles(),
                                                                                        role),
                                                                             _.includes(authorization.operate().roles(),
                                                                                        role),
                                                                             _.includes(authorization.admin().roles(),
                                                                                        role))
    ));
  }

  private initializeErrors(authorization: Authorization) {
    let errorsOnRoles = _.concat(authorization.view().errors().errors("roles") as string[],
                                 authorization.operate().errors().errors("roles") as string[],
                                 authorization.admin().errors().errors("roles") as string[]);
    errorsOnRoles     = _.uniq(errorsOnRoles);
    errorsOnRoles.forEach((err) => {
      if (err && err.length > 0) {
        this.errors().add("roles", err);
      }
    });

    let errorsOnUsers = _.concat(authorization.view().errors().errors("users") as string[],
                                 authorization.operate().errors().errors("users") as string[],
                                 authorization.admin().errors().errors("users") as string[]);

    errorsOnUsers = _.uniq(errorsOnUsers);
    errorsOnUsers.forEach((err) => {
      if (err && err.length > 0) {
        this.errors().add("users", err);
      }
    });
  }
}
