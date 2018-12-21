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

export interface UsersJSON {
  _embedded: EmbeddedJSON;
}

interface EmbeddedJSON {
  users: UserJSON[];
}

export interface UserJSON {
  login_name: string;
  display_name?: string;
  enabled?: boolean;
  email?: string;
  email_me?: boolean;
  checkin_aliases?: string[];
}

export class User {
  loginName: string;
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
    this.checkinAliases = json.checkin_aliases;
  }

  static fromJSON(json: UserJSON) {
    return new User(json);
  }
}

export class Users {
  users: User[];

  constructor(users: User[]) {
    this.users = users;
  }

  static fromJSON(json: UsersJSON) {
    return new Users(json._embedded.users.map((userJson) => User.fromJSON(userJson)));
  }

  list() {
    return this.users;
  }

  enabledUsers() {
    return _.filter(this.users, (user) => user.enabled);
  }

  disabledUsers() {
    return _.filter(this.users, (user) => !user.enabled);
  }

  enabledUsersCount() {
    return this.enabledUsers().length;
  }

  disabledUsersCount() {
    return this.disabledUsers().length;
  }
}
