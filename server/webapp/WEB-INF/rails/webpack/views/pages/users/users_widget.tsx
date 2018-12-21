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
import {User, Users} from "models/users/users";
import {FlashMessage, FlashMessageModel, MessageType} from "views/components/flash_message";
import {Table} from "views/components/table";

interface Attrs {
  users: Stream<Users>;
  message: FlashMessageModel;
}

export class UsersTableWidget extends MithrilViewComponent<Attrs> {
  static headers() {
    return ["Username", "Display name", "Roles", "Admin", "Email", "Enabled"];
  }

  static userData(users: Users): any[][] {
    return users.list().map((user: User) => {
      return [
        user.loginName,
        user.displayName,
        undefined,
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

export class UsersActions extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {

    return [
      <div>Enabled <span data-test-id="enabled-user-count">{vnode.attrs.users().enabledUsersCount()}</span></div>,
      <div>Disabled <span data-test-id="disabled-user-count">{vnode.attrs.users().disabledUsersCount()}</span></div>,
    ];
  }
}

export class UsersWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let optionalMessage: JSX.Element | null = null;
    if (vnode.attrs.message.hasMessage()) {
      optionalMessage =
        <FlashMessage type={vnode.attrs.message.type as MessageType} message={vnode.attrs.message.message}/>;
    }

    if (_.isEmpty(vnode.attrs.users().list())) {
      return (<div>
        {optionalMessage}
        <div>No users found!</div>
      </div>);
    }

    return [
      optionalMessage,
      <UsersActions {...vnode.attrs} />,
      <UsersTableWidget {...vnode.attrs}/>
    ];
  }
}
