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
  checked = Stream(false);
  loginName: Stream<string>;
  isAdmin: Stream<boolean>;
  displayName: Stream<string | undefined>;
  email: Stream<string | undefined>;
  emailMe: Stream<boolean | undefined>;
  enabled: Stream<boolean | undefined>;
  checkinAliases: Stream<string[] | undefined>;
  roles: Stream<UserRoleJSON[] | undefined>;

  constructor(json: UserJSON) {
    this.loginName      = Stream(json.login_name);
    this.displayName    = Stream(json.display_name);
    this.enabled        = Stream(json.enabled);
    this.email          = Stream(json.email);
    this.emailMe        = Stream(json.email_me);
    this.isAdmin        = Stream(json.is_admin);
    this.checkinAliases = Stream(json.checkin_aliases);
    this.roles          = Stream(json.roles);
  }

  static fromJSON(json: UserJSON) {
    return new User(json);
  }

  static clone(existingUser: User) {
    return new User({
                      login_name: existingUser.loginName(),
                      display_name: existingUser.displayName(),
                      enabled: existingUser.enabled(),
                      email: existingUser.email(),
                      email_me: existingUser.emailMe(),
                      is_admin: existingUser.isAdmin(),
                      checkin_aliases: existingUser.checkinAliases()
                    });
  }

  matches(searchText: string) {
    searchText               = searchText.toLowerCase();
    const matchesLoginName   = this.loginName().toLowerCase().includes(searchText);
    const matchesDisplayName = this.displayName() ? this.displayName()!.toLowerCase().includes(searchText) : false;
    const matchesEmail       = this.email() ? this.email()!.toLowerCase().includes(searchText) : false;

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
    return new Users(..._.filter(this, (user) => user.checked()));
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

  sortedByUsername() {
    return new Users(...(_.orderBy(this, (user) => {
      return user.loginName();
    })));
  }

  replace(user: User) {
    const index = this.findIndex((eachUser) => {
      return eachUser.loginName() === user.loginName();
    });

    if (index >= 0) {
      this.splice(index, 1, user);
    }
  }

  enabledUsers() {
    return new Users(..._.filter(this, (user) => user.enabled()!));
  }

  private disabledUsers() {
    return new Users(..._.filter(this, (user) => !user.enabled()));
  }

  private setSelection(newSelection: boolean) {
    this.forEach((user) => user.checked(newSelection));
  }
}
