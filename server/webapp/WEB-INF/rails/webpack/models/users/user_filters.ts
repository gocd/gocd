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

import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {User, Users} from "models/users/users";

export class UserFilters {
  searchText: Stream<string>     = stream("");
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

  performFilteringOn(users: Users) {
    const filteredUsersBasedOnText = _.filter(users, (user) => user.matches(this.searchText()));

    if (this.anyFiltersApplied()) {
      const filteredUsers = _.filter(filteredUsersBasedOnText, (user) => this.applyFiltersOnUser(user));
      return new Users(...filteredUsers);
    } else {
      return new Users(...filteredUsersBasedOnText);

    }

  }

  private applyFiltersOnUser(user: User): boolean {
    let filterOnPrivileges: boolean = !this.isAnyPrivilegesFilterApplied();
    if (this.superAdmins()) {
      filterOnPrivileges = filterOnPrivileges || user.isAdmin();
    }

    if (this.normalUsers()) {
      filterOnPrivileges = filterOnPrivileges || !user.isAdmin();
    }

    let filterOnUserState = !this.isAnyUserStateFilterApplied();
    if (this.enabledUsers()) {
      filterOnUserState = filterOnUserState || user.enabled();
    }

    if (this.disabledUsers()) {
      filterOnUserState = filterOnUserState || !user.enabled();
    }

    return filterOnPrivileges && filterOnUserState;
  }
}
