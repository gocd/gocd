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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {UserJSON} from "models/users/users";
import {Table} from "views/components/table";

interface Attrs {
  users: Stream<UserJSON[]>;
}

export class UsersTableWidget extends MithrilViewComponent<Attrs> {
  static headers() {
    return ["Username", "Display name", "Roles", "Aliases", "Admin", "Email", "Enabled"];
  }

  static userData(users: UserJSON[]): any[][] {
    return users.map((user) => {
      return [
        user.login_name,
        user.display_name,
        undefined, _.join(user.checkin_aliases, ", "),
        undefined,
        user.email,
        user.enabled
      ];
    });
  }

  view(vnode: m.Vnode<Attrs>) {
    return <Table headers={UsersTableWidget.headers()} data={UsersTableWidget.userData(vnode.attrs.users())}/>;
  }

}

export class UsersTableActions extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const [enabledUsers, disabledUsers] = _.partition(vnode.attrs.users(), (user) => {
      return user.enabled;
    });

    return [
      <div>Enabled <span data-test-id="enabled-user-count">{enabledUsers.length}</span></div>,
      <div>Disabled <span data-test-id="disabled-user-count">{disabledUsers.length}</span></div>,
    ];
  }
}

export class UsersWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    if (_.isEmpty(vnode.attrs.users())) {
      return (<div>No users found!</div>);
    }

    return [<UsersTableActions {...vnode.attrs} />, <UsersTableWidget {...vnode.attrs}/>];
  }
}
