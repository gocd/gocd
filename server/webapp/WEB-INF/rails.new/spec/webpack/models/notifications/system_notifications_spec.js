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
describe('SystemNotifications', () => {

  const s = require('string-plus');

  const SystemNotifications = require('models/notifications/system_notifications');

  const notificationJSON     = {
          "id": "id1",
          "message": "message 1.",
          "read": false,
          "type": "UpdateCheck",
          "link": "link_1",
          "linkText": "read more"
      };
  const allNotificationsJSON = [
    {
        "id": "id1",
        "message": "message 1.",
        "read": false,
        "type": "UpdateCheck",
        "link": "link_1",
        "linkText": "read more"
    },
    {
        "id": "id2",
        "message": "message 2.",
        "read": false,
        "type": "SomethingOfImportance",
        "link": "link_2",
        "linkText": "read more"
    }
];
    beforeEach(() => {
        localStorage.clear();
    });

  it('should deserialize the notifications from JSON', () => {
    const allNotifications = SystemNotifications.fromJSON(allNotificationsJSON);

    expect(allNotifications.countSystemNotification()).toBe(2);
    expect(allNotifications.firstSystemNotification().id()).toBe("id1");
    expect(allNotifications.firstSystemNotification().type()).toBe("UpdateCheck");
    expect(allNotifications.firstSystemNotification().link()).toBe("link_1");
    expect(allNotifications.firstSystemNotification().linkText()).toBe("read more");
    expect(allNotifications.firstSystemNotification().read()).toBe(false);

    expect(allNotifications.lastSystemNotification().id()).toBe("id2");
    expect(allNotifications.lastSystemNotification().type()).toBe("SomethingOfImportance");
    expect(allNotifications.lastSystemNotification().link()).toBe("link_2");
    expect(allNotifications.lastSystemNotification().linkText()).toBe("read more");
    expect(allNotifications.lastSystemNotification().read()).toBe(false);
  });

  it('should deserialize a notification from JSON', () => {
    const notification = SystemNotifications.Notification.fromJSON(notificationJSON);
    expect(notification.id()).toBe(notificationJSON.id);
    expect(notification.message()).toBe(notificationJSON.message);
    expect(notification.read()).toBe(notificationJSON.read);
    expect(notification.type()).toBe(notificationJSON.type);
    expect(notification.link()).toBe(notificationJSON.link);
    expect(notification.linkText()).toBe(notificationJSON.linkText);
  });

  it('should serialize a notification to JSON', () => {
    const notification = SystemNotifications.Notification.fromJSON(notificationJSON);
    expect(JSON.parse(JSON.stringify(notification, s.snakeCaser))).toEqual({"id":"id1","message":"message 1.","read":false,"type":"UpdateCheck","link":"link_1","link_text":"read more"});
  });

  describe("all", () => {
      it('should get all notifications', () => {
        localStorage.setItem('system_notifications', JSON.stringify(allNotificationsJSON));
        const successCallback = jasmine.createSpy().and.callFake((notifications) => {
          expect(notifications.countSystemNotification()).toBe(2);
          expect(notifications.firstSystemNotification().id()).toBe("id1");
          expect(notifications.firstSystemNotification().message()).toBe("message 1.");
          expect(notifications.lastSystemNotification().id()).toBe("id2");
          expect(notifications.lastSystemNotification().message()).toBe("message 2.");
        });

        SystemNotifications.all().then(successCallback);
        expect(successCallback).toHaveBeenCalled();
      });
    });

  describe("notifyNewMessage", () => {
      it('should create a new notification in localstorage', () => {
        SystemNotifications.notifyNewMessage(notificationJSON.type, notificationJSON.message, notificationJSON.link, notificationJSON.linkText);
        const fromBrowserLocalStorage = JSON.parse(localStorage.getItem('system_notifications'));
        expect(fromBrowserLocalStorage.length).toBe(1);
        expect(fromBrowserLocalStorage[0].message).toBe(notificationJSON.message);
        expect(fromBrowserLocalStorage[0].type).toBe(notificationJSON.type);
        expect(fromBrowserLocalStorage[0].link).toBe(notificationJSON.link);
        expect(fromBrowserLocalStorage[0].linkText).toBe(notificationJSON.linkText);
        expect(fromBrowserLocalStorage[0].id).not.toBeUndefined();
      });

      it('should not create duplicate notifications for a given notification type in localstorage, it should update the existing one', () => {
        SystemNotifications.notifyNewMessage(notificationJSON.type, notificationJSON.message, notificationJSON.link, notificationJSON.linkText);
        SystemNotifications.notifyNewMessage(notificationJSON.type, notificationJSON.message, notificationJSON.link, notificationJSON.linkText);
        const fromBrowserLocalStorage = JSON.parse(localStorage.getItem('system_notifications'));
        expect(fromBrowserLocalStorage.length).toBe(1);
        expect(fromBrowserLocalStorage[0].message).toBe(notificationJSON.message);
        expect(fromBrowserLocalStorage[0].type).toBe(notificationJSON.type);
        expect(fromBrowserLocalStorage[0].link).toBe(notificationJSON.link);
        expect(fromBrowserLocalStorage[0].linkText).toBe(notificationJSON.linkText);
        expect(fromBrowserLocalStorage[0].id).not.toBeUndefined();
      });
  });

  describe("SystemNotifications.Notification.markAsRead", () => {
      it('should mark a new notification as read in localstorage', () => {
        localStorage.setItem('system_notifications', JSON.stringify(allNotificationsJSON));
        const notification = SystemNotifications.Notification.fromJSON(allNotificationsJSON[0]);
        notification.markAsRead();

        const reloadedSystemNotifications = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));

        const actualNotification1 = reloadedSystemNotifications.findSystemNotification( (n) => {
            return n.id() === allNotificationsJSON[0].id;
        });
        expect(actualNotification1.message()).toBe(allNotificationsJSON[0].message);
        expect(actualNotification1.read()).toBe(true);

        const actualNotification2 = reloadedSystemNotifications.findSystemNotification( (n) => {
            return n.id() === allNotificationsJSON[1].id;
        });
        expect(actualNotification2.message()).toBe(allNotificationsJSON[1].message);
        expect(actualNotification2.read()).toBe(false);
      });
  });
});
