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

import {TestHelper} from "views/pages/spec/test_helper";

describe("SystemNotificationsWidget", () => {
  const m             = require("mithril");
  const Stream        = require("mithril/stream");
  const simulateEvent = require('simulate-event');
  const helper        = new TestHelper();

  require('jasmine-jquery');
  require('jasmine-ajax');

  const SystemNotificationsWidget = require('views/notifications/system_notifications_widget');
  const SystemNotifications       = require('models/notifications/system_notifications').SystemNotifications;

  afterEach(helper.unmount.bind(helper));

  const systemNotifications = Stream(new SystemNotifications());

  beforeEach(() => {
    helper.mount(() => m(SystemNotificationsWidget, {
      systemNotifications
    }));
    localStorage.clear();
    localStorage.setItem('system_notifications', JSON.stringify([]));
  });

  it("should not display notification bell when there are no notifications to display", () => {
    systemNotifications(SystemNotifications.fromJSON([]));
    helper.redraw(true);
    expect(helper.find('.notifications').get(0)).not.toBeInDOM();
  });

  it("should display notification bell and the notifications", () => {
    const notifications = [
      {
        "id":       "id1",
        "message":  "message 1.",
        "read":     false,
        "type":     "UpdateCheck",
        "link":     "link_1",
        "linkText": "read more"
      },
      {
        "id":       "id2",
        "message":  "message 2.",
        "read":     false,
        "type":     "SomethingOfImportance",
        "link":     "link_2",
        "linkText": "read more"
      }
    ];

    systemNotifications(SystemNotifications.fromJSON(notifications));
    helper.redraw(true);
    expect(helper.find('.notifications').get(0)).toBeInDOM();
    expect(helper.find('.notifications .bell').get(0)).toBeInDOM();
    expect(helper.find('.notifications .hover-container').get(0)).toBeInDOM();
    expect(helper.find('.notifications .hover-container .notification-hover').get(0)).toBeInDOM();
    expect(helper.find('.notifications .hover-container .notification-hover p').get(0)).toBeInDOM();
    const allNotifications = helper.find('.notifications .hover-container .notification-hover p');
    expect(allNotifications.length).toBe(2);
    expect(allNotifications.eq(0)).toContainText("message 1. read more");
    expect(allNotifications.eq(0).find('a')).toContainText("read more");
    expect(allNotifications.eq(0).find('span.close')).toContainText("X");
    expect(allNotifications.eq(1)).toContainText("message 2. read more");
    expect(allNotifications.eq(1).find('a')).toContainText("read more");
    expect(allNotifications.eq(1).find('span.close')).toContainText("X");
  });


  it("should mark a notification as read and stop displaying it when user marks a notification as read", () => {
    const notifications = [
      {
        "id":       "id1",
        "message":  "message 1.",
        "read":     false,
        "type":     "UpdateCheck",
        "link":     "link_1",
        "linkText": "read more"
      },
      {
        "id":       "id2",
        "message":  "message 2.",
        "read":     false,
        "type":     "SomethingOfImportance",
        "link":     "link_2",
        "linkText": "read more"
      }
    ];

    systemNotifications(SystemNotifications.fromJSON(notifications));
    helper.redraw(true);
    let allNotifications = helper.find('.notifications .hover-container .notification-hover p');
    expect(allNotifications.length).toBe(2);
    simulateEvent.simulate(allNotifications.eq(0).find('span.close').get(0), 'click');
    helper.redraw(true);

    allNotifications = helper.find('.notifications .hover-container .notification-hover p');
    expect(allNotifications.length).toBe(1);
    expect(allNotifications.eq(0)).toContainText("message 2. read more");

    expect(systemNotifications().find(() => {
      return helper.id === "id1";
    })).toBeUndefined();
    const remainingNotification = systemNotifications().find((m) => {
      return m.id === "id2";
    });
    expect(remainingNotification).not.toBeUndefined();
    expect(remainingNotification.message).toBe("message 2.");
    expect(remainingNotification.read).toBe(false);
  });
});
