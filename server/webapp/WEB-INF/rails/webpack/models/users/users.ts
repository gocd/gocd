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

export interface UserJSON {
  login_name: string;
  is_admin: boolean;
  display_name?: string;
  enabled?: boolean;
  email?: string;
  email_me?: boolean;
  checkin_aliases?: string[];
}

export class User {
  checked: Stream<boolean> = stream(false);
  loginName: string;
  isAdmin: boolean;
  displayName: string | undefined;
  email: string | undefined;
  emailMe: boolean | undefined;
  enabled: boolean | undefined;
  checkinAliases: string[] | undefined;

  constructor(json: UserJSON) {
    this.loginName      = json.login_name;
    this.displayName    = json.display_name;
    this.enabled        = json.enabled;
    this.email          = json.email;
    this.emailMe        = json.email_me;
    this.isAdmin        = json.is_admin;
    this.checkinAliases = json.checkin_aliases;
  }

  static fromJSON(json: UserJSON) {
    return new User(json);
  }
}

export class UserFilters {
  superAdmins: Stream<boolean>   = stream(false);
  normalUsers: Stream<boolean>   = stream(false);
  enabledUsers: Stream<boolean>  = stream(false);
  disabledUsers: Stream<boolean> = stream(false);

  resetFilters() {
    this.superAdmins(false);
    this.normalUsers(false);
    this.enabledUsers(false);
    this.disabledUsers(false);
  }

  isAnyPrivilegesFilterApplied() {
    return this.superAdmins() || this.normalUsers();
  }

  isAnyUserStateFilterApplied() {
    return this.enabledUsers() || this.disabledUsers();
  }

  anyFiltersApplied() {
    return this.isAnyPrivilegesFilterApplied() || this.isAnyUserStateFilterApplied();
  }

  applyFiltersOnUser(user: User): boolean {
    let filterOnPrivileges = !this.isAnyPrivilegesFilterApplied();
    if (this.superAdmins()) {
      filterOnPrivileges = filterOnPrivileges || user.isAdmin;
    }

    if (this.normalUsers()) {
      filterOnPrivileges = filterOnPrivileges || !user.isAdmin;
    }

    let filterOnUserState = !this.isAnyUserStateFilterApplied();
    if (this.enabledUsers()) {
      filterOnUserState = filterOnUserState || user.enabled as boolean;
    }

    if (this.disabledUsers()) {
      filterOnUserState = filterOnUserState || !user.enabled;
    }

    return filterOnPrivileges && filterOnUserState;
  }
}

export class Users {

  search: Stream<string> = stream("");
  filters: UserFilters   = new UserFilters();
  private readonly __originalUsers: User[];

  constructor(users: User[]) {
    this.__originalUsers = users;
  }

  static fromJSON(json: UsersJSON) {
    return new Users(json._embedded.users.map((userJson) => User.fromJSON(userJson)));
  }

  users() {
    return this.applySearch(this.applyFilters(this.__originalUsers));
  }

  list() {
    return this.users();
  }

  enabledUsersCount() {
    return this.enabledUsers().length;
  }

  totalUsersCount() {
    return this.users().length;
  }

  disabledUsersCount() {
    return this.disabledUsers().length;
  }

  areAllUsersSelected() {
    return this.users().every((user) => user.checked());
  }

  toggleSelection() {
    this.setSelection(!this.areAllUsersSelected());
  }

  private applyFilters(users: User[]): User[] {
    return this.filters.anyFiltersApplied() ? users.filter(this.filters.applyFiltersOnUser.bind(this.filters)) : users;
  }

  private applySearch(users: User[]): User[] {
    return users.filter((user: User) => {
      const matchesLoginName   = user.loginName.includes(this.search());
      const matchesDisplayName = user.displayName ? user.displayName.includes(this.search()) : false;
      const matchesEmail       = user.email ? user.email.includes(this.search()) : false;

      return (matchesLoginName || matchesDisplayName || matchesEmail);
    });
  }

  private enabledUsers() {
    return _.filter(this.users(), (user) => user.enabled);
  }

  private disabledUsers() {
    return _.filter(this.users(), (user) => !user.enabled);
  }

  private setSelection(newSelection: boolean) {
    this.users().forEach((user) => user.checked(newSelection));
  }
}
