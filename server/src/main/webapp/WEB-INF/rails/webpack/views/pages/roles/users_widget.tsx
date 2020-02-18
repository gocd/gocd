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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {GoCDAttributes} from "models/roles/roles";
import styles from "./index.scss";

interface UsersWidgetAttrs {
  roleAttributes: Stream<GoCDAttributes>;
  selectedUser?: string;
  readOnly?: boolean;
}

export class UsersWidget extends MithrilViewComponent<UsersWidgetAttrs> {
  view(vnode: m.Vnode<UsersWidgetAttrs>): m.Children | void | null {
    return vnode.attrs.roleAttributes().users.map((user) => {
      const isSelectedUser = user === vnode.attrs.selectedUser;
      const classForUser   = isSelectedUser ? `${styles.tag} ${styles.currentUserTag}` : `${styles.tag}`;
      let mayBeDeleteButton;
      if (!UsersWidget.readOnlyView(vnode)) {
        mayBeDeleteButton = (
          <span aria-hidden="true" class={styles.roleUserDeleteIcon}
                onclick={UsersWidget.deleteUserFromRole.bind(this,
                                                             vnode.attrs.roleAttributes,
                                                             user)}>&times;</span>);
      }
      return (
        <div data-alert class={classForUser}>
          <span>{user}</span>
          {mayBeDeleteButton}
        </div>
      );
    });
  }

  private static deleteUserFromRole(attributes: Stream<GoCDAttributes>, username: string) {
    if (username) {
      attributes().deleteUser(username);
    }
  }

  private static readOnlyView(vnode: m.Vnode<UsersWidgetAttrs>) {
    return !(false === vnode.attrs.readOnly);
  }
}
