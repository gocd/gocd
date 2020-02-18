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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Notification, SystemNotifications} from "models/notifications/system_notifications";
import {SystemNotificationWidget} from "views/components/notification_center/system_notification_widget";
import styles from "./system_notifications.scss";

interface Attrs {
  systemNotifications: Stream<SystemNotifications>;
}

interface State {
  markAsRead: (notification: Notification) => void;
}

export class SystemNotificationsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.markAsRead = (notification: Notification) => {
      notification.markAsRead();

      vnode.attrs.systemNotifications().remove((e: Notification) => {
        return notification.id === e.id;
      });

      m.redraw();
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    if (vnode.attrs.systemNotifications().count() === 0) {
      return (<div/>);
    }

    return (
      <div class={styles.notifications}>
        <span class={styles.bell}></span>
        <div class={styles.hoverContainer}>
          <div class={styles.notificationHover}>
            {
              vnode.attrs.systemNotifications().map((notification: Notification) => {
                return (
                  <SystemNotificationWidget
                    notification={notification}
                    markAsRead={vnode.state.markAsRead.bind(vnode.state, notification)}/>
                );
              })
            }
          </div>
        </div>
      </div>
    );
  }
}
