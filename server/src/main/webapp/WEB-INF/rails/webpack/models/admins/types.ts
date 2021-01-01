/*
 * Copyright 2021 ThoughtWorks, Inc.
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

export interface SystemAdminsJSON {
  roles: string[];
  users: string[];
}

export class SystemAdmins {
  readonly users: Stream<string[]>;
  readonly roles: Stream<string[]>;

  constructor(users: string[], roles: string[]) {
    this.users = Stream(users);
    this.roles = Stream(roles);
  }

  static fromJSON(json: SystemAdminsJSON) {
    return new SystemAdmins(json.users, json.roles);
  }

  noAdminsConfigured(): boolean {
    return _.isEmpty(this.users()) && _.isEmpty(this.roles());
  }

  isIndividualAdmin(username: string) {
    return _.findIndex(this.users(), (eachUser) => eachUser.toLowerCase() === username.toLowerCase()) !== -1;
  }
}
