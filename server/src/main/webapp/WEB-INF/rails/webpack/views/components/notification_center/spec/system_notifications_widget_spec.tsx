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
import "jasmine-ajax";
import m from "mithril";
import Stream from "mithril/stream";
import {Notification, SystemNotifications} from "models/notifications/system_notifications";
import {SystemNotificationsWidget} from "views/components/notification_center/system_notifications_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("SystemNotificationsWidget", () => {

  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  const systemNotifications = Stream(new SystemNotifications());

  beforeEach(() => {
    helper.mount(() => <SystemNotificationsWidget systemNotifications={systemNotifications}/>);
    localStorage.clear();
    localStorage.setItem("system_notifications", JSON.stringify([]));
  });

  it("should not display notification bell when there are no notifications to display", () => {
    systemNotifications(SystemNotifications.fromJSON([]));
    helper.redraw();
    expect(helper.q(".notifications")).toBeFalsy();
  });

  it("should display notification bell and the notifications", () => {
    const notifications = [
      {
        id: "id1",
        message: "message 1.",
        read: false,
        type: "UpdateCheck",
        link: "link_1",
        linkText: "read more"
      },
      {
        id: "id2",
        message: "message 2.",
        read: false,
        type: "SomethingOfImportance",
        link: "link_2",
        linkText: "read more"
      }
    ] as Notification[];

    systemNotifications(SystemNotifications.fromJSON(notifications));
    helper.redraw();
    const allNotifications = helper.allByTestId("notification-item");
    expect(allNotifications.item(0)).toBeInDOM();
    expect(allNotifications.length).toBe(2);
    expect(allNotifications.item(0)).toContainText("message 1. read more");
    expect(helper.q("a", allNotifications.item(0))).toContainText("read more");
    expect(helper.byTestId("notification-item_close", allNotifications.item(0))).toContainText("X");
    expect(allNotifications.item(1)).toContainText("message 2. read more");
    expect(helper.q("a", allNotifications.item(1))).toContainText("read more");
    expect(helper.allByTestId("notification-item_close", allNotifications.item(1))).toContainText("X");
  });

  it("should mark a notification as read and stop displaying it when user marks a notification as read", () => {
    const notifications = [
      {
        id: "id1",
        message: "message 1.",
        read: false,
        type: "UpdateCheck",
        link: "link_1",
        linkText: "read more"
      },
      {
        id: "id2",
        message: "message 2.",
        read: false,
        type: "SomethingOfImportance",
        link: "link_2",
        linkText: "read more"
      }
    ] as Notification[];

    systemNotifications(SystemNotifications.fromJSON(notifications));
    helper.redraw();
    let allNotifications = helper.allByTestId("notification-item");
    expect(allNotifications.length).toBe(2);

    helper.clickByTestId("notification-item_close", allNotifications.item(0));

    allNotifications = helper.allByTestId("notification-item");
    expect(allNotifications.length).toBe(1);
    expect(allNotifications.item(0)).toContainText("message 2. read more");

    expect(systemNotifications().find((m: Notification) => {
      return m.id === "id1";
    })).toBeUndefined();

    const remainingNotification = systemNotifications().find((notification: Notification) => {
      return notification.id === "id2";
    }) as Notification;

    expect(remainingNotification).not.toBeUndefined();
    expect(remainingNotification.message).toBe("message 2.");
    expect(remainingNotification.read).toBe(false);
  });

});
