/*
* Copyright 2018 ThoughtWorks, Inc.
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

import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";

export interface UsersJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  users: UserJSON[];
}

export interface UserStateUpdateOperationJSON {
  enable: boolean;
}

export interface BulkUserOperationJSON {
  users: string[];
}

export interface BulkUserUpdateJSON extends BulkUserOperationJSON {
  operations: UserStateUpdateOperationJSON;
}

export interface UserRoleJSON {
  name: string;
  type: "gocd" | "plugin";
}

export interface UserJSON {
  login_name: string;
  is_admin: boolean;
  display_name?: string;
  enabled?: boolean;
  email?: string;
  email_me?: boolean;
  checkin_aliases?: string[];
  roles?: UserRoleJSON[];
}

export class User {
  checked: Stream<boolean> = stream(false);
  loginName: Stream<string>;
  isAdmin: Stream<boolean>;
  displayName: Stream<string>;
  email: Stream<string>;
  emailMe: Stream<boolean>;
  enabled: Stream<boolean>;
  checkinAliases: Stream<string[]>;
  roles: Stream<UserRoleJSON[]>;

  constructor(json: UserJSON) {
    this.loginName      = stream(json.login_name);
    this.displayName    = stream(json.display_name);
    this.enabled        = stream(json.enabled);
    this.email          = stream(json.email);
    this.emailMe        = stream(json.email_me);
    this.isAdmin        = stream(json.is_admin);
    this.checkinAliases = stream(json.checkin_aliases);
    this.roles          = stream(json.roles);
  }

  static fromJSON(json: UserJSON) {
    return new User(json);
  }

  matches(searchText: string) {
    const matchesLoginName   = this.loginName().includes(searchText);
    const matchesDisplayName = this.displayName() ? this.displayName().includes(searchText) : false;
    const matchesEmail       = this.email() ? this.email().includes(searchText) : false;

    return (matchesLoginName || matchesDisplayName || matchesEmail);
  }

  gocdRoles() {
    return _(this.roles()).filter((role) => "gocd" === role.type).map((role) => role.name).value();
  }

  pluginRoles() {
    return _(this.roles()).filter((role) => "gocd" !== role.type).map((role) => role.name).value();
  }
}

export class Users extends Array<User> {
  constructor(...users: User[]) {
    super(...users);
    Object.setPrototypeOf(this, Object.create(Users.prototype));
  }

  static fromJSON(json: UsersJSON) {
    return new Users(...json._embedded.users.map((userJson) => User.fromJSON(userJson)));
  }

  enabledUsersCount() {
    return this.enabledUsers().length;
  }

  totalUsersCount() {
    return this.length;
  }

  disabledUsersCount() {
    return this.disabledUsers().length;
  }

  areAllUsersSelected() {
    return this.every((user) => user.checked());
  }

  selectedUsers() {
    return _.filter(this, (user) => user.checked());
  }

  userNamesOfSelectedUsers() {
    return _.map(this.selectedUsers(), (user) => user.loginName());
  }

  anyUserSelected() {
    return this.selectedUsers().length > 0;
  }

  toggleSelection() {
    this.setSelection(!this.areAllUsersSelected());
  }

  private enabledUsers() {
    return _.filter(this, (user) => user.enabled());
  }

  private disabledUsers() {
    return _.filter(this, (user) => !user.enabled());
  }

  private setSelection(newSelection: boolean) {
    this.forEach((user) => user.checked(newSelection));
  }
}
