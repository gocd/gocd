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
describe('DataSharingNotification', () => {
  const DataSharingNotification = require('models/notifications/data_sharing_notification');
  const SystemNotifications = require('models/notifications/system_notifications');

  beforeEach(() => {
      localStorage.clear();
  });

  it('should create a notification for data sharing', () => {
    DataSharingNotification.createIfNotPresent();
    const notifications = JSON.parse(localStorage.getItem('system_notifications'));
    expect(notifications.length).toBe(1);
    expect(notifications[0].message).toBe("GoCD shares data so that it can be improved.");
    expect(notifications[0].type).toBe("DataSharing");
    expect(notifications[0].link).toBe("/go/admin/data_sharing/settings");
    expect(notifications[0].linkText).toBe("Learn more ...");
    expect(notifications[0].read).toBe(false);
    expect(notifications[0].id).not.toBeUndefined();
  });

  it('should not create a notification for data sharing if one already exists in the store', () => {
    DataSharingNotification.createIfNotPresent();

    const allNotifications = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));

    const dataSharingNotification = allNotifications.findSystemNotification((m) => {
        return m.type() === 'DataSharing';
    });
    expect(dataSharingNotification).not.toBeUndefined();
    expect(dataSharingNotification.message()).toBe("GoCD shares data so that it can be improved.");
    expect(dataSharingNotification.type()).toBe("DataSharing");
    expect(dataSharingNotification.link()).toBe("/go/admin/data_sharing/settings");
    expect(dataSharingNotification.linkText()).toBe("Learn more ...");
    expect(dataSharingNotification.read()).toBe(false);
    expect(dataSharingNotification.id()).not.toBeUndefined();


    DataSharingNotification.createIfNotPresent();
    const allNotificationsAfterSecondCreateCall = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));
    const dataSharingNotificationAfterSecondCreateCall = allNotificationsAfterSecondCreateCall.findSystemNotification((m) => {
        return m.type() === 'DataSharing';
    });
    expect(dataSharingNotificationAfterSecondCreateCall).not.toBeUndefined();
    expect(dataSharingNotificationAfterSecondCreateCall.id()).toBe(dataSharingNotification.id());
  });
});
