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
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {Notification} from "models/notifications/system_notifications";
import * as style from "./system_notifications.scss";

interface Attrs {
  markAsRead: () => void;
  notification: Notification;
}

export class SystemNotificationWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const notification = vnode.attrs.notification;

    return (
      <p class={style.notificationItem} data-test-id="notification-item">
        {notification.message} <a href={notification.link} target="_blank">{notification.linkText}</a>
        <span onclick={vnode.attrs.markAsRead.bind(vnode.attrs, notification)}
              data-test-id="notification-item_close" class={style.notificationClose}>X</span>
      </p>
    );
  }
}
