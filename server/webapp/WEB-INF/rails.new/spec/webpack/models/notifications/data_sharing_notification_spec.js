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
  const SystemNotifications     = require('models/notifications/system_notifications');

  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create a notification for data sharing if one does not exist', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/go/api/data_sharing/settings/notification_auth").andReturn({
        responseText:    JSON.stringify({"show_notification": true}),
        status:          200,
        responseHeaders: {
          'Accept':       'application/vnd.go.cd.v1+json',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake(() => {
        const notifications = JSON.parse(localStorage.getItem('system_notifications'));
        expect(notifications.length).toBe(1);
        expect(notifications[0].message).toBe("GoCD shares data so that it can be improved.");
        expect(notifications[0].type).toBe("DataSharing_v18.8.0");
        expect(notifications[0].link).toBe("/go/admin/data_sharing/settings");
        expect(notifications[0].linkText).toBe("Learn more ...");
        expect(notifications[0].read).toBe(false);
        expect(notifications[0].id).not.toBeUndefined();

        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/data_sharing/settings/notification_auth');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });

      DataSharingNotification.createIfNotPresent().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should create a notification for updated metrics data sharing when a notification for data sharing already exist', () => {
    addExistingDataSharingNotification();

    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/go/api/data_sharing/settings/notification_auth").andReturn({
        responseText:    JSON.stringify({"show_notification": true}),
        status:          200,
        responseHeaders: {
          'Accept':       'application/vnd.go.cd.v1+json',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake(() => {
        const notifications = JSON.parse(localStorage.getItem('system_notifications'));

        expect(notifications.length).toBe(2);

        const updatedMetricsdataSharingNotification = SystemNotifications.fromJSON(notifications).findSystemNotification((m) => {
          return m.type() === 'DataSharing_v18.8.0';
        });

        expect(updatedMetricsdataSharingNotification.message()).toBe("GoCD’s shared data has been updated to include new metrics.");
        expect(updatedMetricsdataSharingNotification.type()).toBe("DataSharing_v18.8.0");
        expect(updatedMetricsdataSharingNotification.link()).toBe("/go/admin/data_sharing/settings");
        expect(updatedMetricsdataSharingNotification.linkText()).toBe("Learn more ...");
        expect(updatedMetricsdataSharingNotification.read()).toBe(false);
        expect(updatedMetricsdataSharingNotification.id()).not.toBeUndefined();

        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/data_sharing/settings/notification_auth');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });

      DataSharingNotification.createIfNotPresent().then(successCallback);
      expect(successCallback).toHaveBeenCalled();
    });
  });

  it('should not create a notification for data sharing if one already exists in the store', () => {
    let dataSharingNotification;
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/go/api/data_sharing/settings/notification_auth").andReturn({
        responseText:    JSON.stringify({"show_notification": true}),
        status:          200,
        responseHeaders: {
          'Accept':       'application/vnd.go.cd.v1+json',
          'Content-Type': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake(() => {
        const allNotifications  = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));
        dataSharingNotification = allNotifications.findSystemNotification((m) => {
          return m.type() === 'DataSharing_v18.8.0';
        });
        expect(dataSharingNotification).not.toBeUndefined();
        expect(dataSharingNotification.message()).toBe("GoCD shares data so that it can be improved.");
        expect(dataSharingNotification.type()).toBe("DataSharing_v18.8.0");
        expect(dataSharingNotification.link()).toBe("/go/admin/data_sharing/settings");
        expect(dataSharingNotification.linkText()).toBe("Learn more ...");
        expect(dataSharingNotification.read()).toBe(false);
        expect(dataSharingNotification.id()).not.toBeUndefined();
      });

      DataSharingNotification.createIfNotPresent().then(successCallback);
      expect(successCallback).toHaveBeenCalled();

      const callbackAfterSecondCallToCreate = jasmine.createSpy().and.callFake(() => {
        const allNotificationsAfterSecondCreateCall        = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));
        const dataSharingNotificationAfterSecondCreateCall = allNotificationsAfterSecondCreateCall.findSystemNotification((m) => {
          return m.type() === 'DataSharing_v18.8.0';
        });
        expect(dataSharingNotificationAfterSecondCreateCall).not.toBeUndefined();
        expect(dataSharingNotificationAfterSecondCreateCall.id()).toBe(dataSharingNotification.id());

      });


      DataSharingNotification.createIfNotPresent().then(callbackAfterSecondCallToCreate);
      expect(callbackAfterSecondCallToCreate).toHaveBeenCalled();
    });
  });


  it('should not create a notification for data sharing if the server doesnot allow a notification for the user', () => {
    jasmine.Ajax.withMock(() => {
      jasmine.Ajax.stubRequest("/go/api/data_sharing/settings/notification_auth").andReturn({
        responseText:    JSON.stringify({"show_notification": false}),
        status:          200,
        responseHeaders: {
          'Accept': 'application/vnd.go.cd.v1+json'
        }
      });

      const successCallback = jasmine.createSpy().and.callFake(() => {
        const allNotifications = SystemNotifications.fromJSON(JSON.parse(localStorage.getItem('system_notifications')));
        expect(allNotifications.countSystemNotification()).toBe(0);
        const request = jasmine.Ajax.requests.mostRecent();
        expect(request.method).toBe('GET');
        expect(request.url).toBe('/go/api/data_sharing/settings/notification_auth');
        expect(request.requestHeaders['Content-Type']).toContain('application/json');
        expect(request.requestHeaders['Accept']).toContain('application/vnd.go.cd.v1+json');
      });

      DataSharingNotification.createIfNotPresent().then(successCallback);
      expect(successCallback).toHaveBeenCalled();

    });
  });

  function addExistingDataSharingNotification() {
    localStorage.clear();

    const dataSharingNotification = {
      "id":       "5a11a2fe-eb1c-44c3-9ff3-863bd921c6d8",
      "message":  "GoCD shares data so that it can be improved.",
      "read":     false,
      "type":     "DataSharing",
      "link":     "/go/admin/data_sharing/settings",
      "linkText": "Learn more ..."
    };

    localStorage.setItem('system_notifications', JSON.stringify([dataSharingNotification]));
  }

});
