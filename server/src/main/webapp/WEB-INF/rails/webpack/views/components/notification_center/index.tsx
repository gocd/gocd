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
import {AjaxPoller} from "helpers/ajax_poller";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {SystemNotifications} from "models/notifications/system_notifications";
import {SystemNotificationsWidget} from "views/components/notification_center/system_notifications_widget";

interface State {
  systemNotifications: Stream<SystemNotifications>;
  repeater: AjaxPoller<void>;
}

export class NotificationCenter extends MithrilComponent<{}, State> {
  oninit(vnode: m.Vnode<{}, State>) {
    vnode.state.repeater            = this.createRepeater(vnode);
    vnode.state.systemNotifications = Stream(new SystemNotifications());
    vnode.state.repeater.start();
  }

  onremove(vnode: m.VnodeDOM<{}, State>) {
    vnode.state.repeater.stop();
  }

  view(vnode: m.Vnode<{}, State>) {
    return <SystemNotificationsWidget systemNotifications={vnode.state.systemNotifications}/>;
  }

  private createRepeater(vnode: m.Vnode<{}, State>) {
    return new AjaxPoller(() => SystemNotifications.all().then((data: SystemNotifications) => {
      vnode.state.systemNotifications(new SystemNotifications(data.filter((n) => !n.read)));
      m.redraw();
    }));
  }
}
