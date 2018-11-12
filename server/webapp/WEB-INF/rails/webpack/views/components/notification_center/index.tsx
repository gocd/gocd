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

import {AjaxPoller} from "helpers/ajax_poller";
import * as m from "mithril";
import * as Stream from "mithril/stream";
import {DataSharingNotification} from "models/notifications/data_sharing_notification";
import {SystemNotifications} from "models/notifications/system_notifications";
import {SystemNotificationsWidget} from "views/components/notification_center/system_notifications_widget";

const systemNotifications = Stream(new SystemNotifications());

function createRepeater() {
  return new AjaxPoller(() => SystemNotifications.all().then(redraw));
}

const repeater = Stream(createRepeater());

const NotificationCenter = {
  oninit() {
    DataSharingNotification.createIfNotPresent();
    repeater().start();
  },
  onbeforeremove() {
    repeater().stop();
  },
  view() {
    return <SystemNotificationsWidget systemNotifications={systemNotifications}/>;
  }
};

const redraw = (data: SystemNotifications) => {
  systemNotifications(new SystemNotifications(data.filter((n) => n.read === false)));
  m.redraw();
};

module.exports = NotificationCenter;
