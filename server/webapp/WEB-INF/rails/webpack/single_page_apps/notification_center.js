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

const $                         = require('jquery');
const m                         = require('mithril');
const Stream                    = require('mithril/stream');
const SystemNotificationsWidget = require('views/notifications/system_notifications_widget');

const SystemNotifications       = require('models/notifications/system_notifications').SystemNotifications;
const DataSharingNotification   = require('models/notifications/data_sharing_notification').DataSharingNotification;
const AjaxPoller                = require('helpers/ajax_poller').AjaxPoller;

require('foundation-sites');

$(() => {
  DataSharingNotification.createIfNotPresent();
  const systemNotifications = Stream(new SystemNotifications());
  Promise.all([SystemNotifications.all()]).then(() => {
    m.mount($("#system-notifications").get(0), {
      view() {
        return (<SystemNotificationsWidget systemNotifications={systemNotifications}/>);
      }
    });
    $(document).foundation();
  });

  const redraw = (data) => {
    systemNotifications(new SystemNotifications(data.filter((n) => n.read === false)));
    m.redraw();
  };

  function createRepeater() {
    return new AjaxPoller(() => SystemNotifications.all().then(redraw));
  }

  const repeater = Stream(createRepeater());
  repeater().start();
});
