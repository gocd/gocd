/*
 * Copyright 2024 Thoughtworks, Inc.
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
import _ from "lodash";
import mockDate from "mockdate";
import {Notification, SystemNotifications} from "models/notifications/system_notifications";
import {VersionUpdater} from "models/shared/version_updater";

describe("VersionUpdater", () => {

  describe("update", () => {
    beforeEach(() => {
      localStorage.clear();
      jasmine.Ajax.install();
    });

    afterEach(() => {
      jasmine.Ajax.uninstall();
      mockDate.reset();
      localStorage.clear();
    });

    it("should fetch the stale version info", async () => {
      jasmine.Ajax.stubRequest("/go/api/version_infos/stale", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/latest_version", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      const thirtyOneMinutesBack = new Date(Date.now() - 31 * 60 * 1000);

      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: thirtyOneMinutesBack}));

      await VersionUpdater.update();

      const request = jasmine.Ajax.requests.at(0);

      expect(request.method).toBe("GET");
      expect(request.url).toBe("/go/api/version_infos/stale");
      expect(request.requestHeaders["Content-Type"]).toContain("application/json");
      expect(request.requestHeaders.Accept).toContain("application/vnd.go.cd.v1+json");

      const nots = JSON.parse(localStorage.getItem("system_notifications") as string);
      expect(_.isNull(nots) ? [] : nots).toHaveLength(0);
    });

    it("should skip updates if update tried in last half hour", async () => {
      const twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);

      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: twentyNineMinutesBack}));

      await VersionUpdater.update();

      expect(jasmine.Ajax.requests.count()).toBe(0);
    });

    //prior to 19.3.0 current_gocd_version wasnt stored in the local storage
    it("update_check should remove older update system notification on upgrading to 19.3.0", async () => {
      const twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);
      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: twentyNineMinutesBack}));

      document.body.setAttribute("data-current-gocd-version", "19.3.0-1234");
      const systemNotifications = new SystemNotifications();
      systemNotifications.add(new Notification(updateNotificationJSON));
      SystemNotifications.setNotifications(systemNotifications);

      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(1);
      await VersionUpdater.update();
      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(0);
      expect(localStorage.getItem(VersionUpdater.CURRENT_GOCD_VERSION_KEY) as string).toBe("19.3.0-1234");
    });

    it("update_check should remove older update system notification on upgrading from 19.3.0 to 19.4.0", async () => {
      const twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);
      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: twentyNineMinutesBack}));

      document.body.setAttribute("data-current-gocd-version", "19.4.0-1234");
      localStorage.setItem(VersionUpdater.CURRENT_GOCD_VERSION_KEY, "19.3.0-1234");
      const systemNotifications = new SystemNotifications();
      systemNotifications.add(new Notification(updateNotificationJSON));
      SystemNotifications.setNotifications(systemNotifications);

      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(1);
      await VersionUpdater.update();
      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(0);
    });

    it("update_check should remove update system notification on downgrading from 19.3.0 to 19.1.0", async () => {
      const twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);
      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: twentyNineMinutesBack}));

      document.body.setAttribute("data-current-gocd-version", "19.1.0-1234");
      localStorage.setItem(VersionUpdater.CURRENT_GOCD_VERSION_KEY, "19.3.0-1234");
      const systemNotifications = new SystemNotifications();
      systemNotifications.add(new Notification(updateNotificationJSON));
      SystemNotifications.setNotifications(systemNotifications);

      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(1);
      await VersionUpdater.update();
      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(0);
    });

    it("update_check should not remove update system notification on for same GoCD version", async () => {
      const twentyNineMinutesBack = new Date(Date.now() - 29 * 60 * 1000);
      localStorage.setItem("versionCheckInfo", JSON.stringify({last_updated_at: twentyNineMinutesBack}));

      document.body.setAttribute("data-current-gocd-version", "19.3.0-1234");
      localStorage.setItem(VersionUpdater.CURRENT_GOCD_VERSION_KEY, "19.3.0-1234");
      const systemNotifications = new SystemNotifications();
      systemNotifications.add(new Notification(updateNotificationJSON));
      SystemNotifications.setNotifications(systemNotifications);

      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(1);
      await VersionUpdater.update();
      expect(JSON.parse(localStorage.getItem("system_notifications") as string)).toHaveLength(1);
    });

    it("should skip updates in absence of stale version info and update local storage with last update time", async () => {
      jasmine.Ajax.stubRequest("/go/api/version_infos/stale", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/latest_version", undefined, "GET").andReturn({
        responseText: JSON.stringify({latest_version: "18.7.0-1234"}),
        status: 200
      });

      mockDate.set("2015-10-15T04:29:00.000Z");

      await VersionUpdater.update();

      expect(jasmine.Ajax.requests.count()).toBe(2);
      expect(localStorage.getItem("versionCheckInfo")).toEqual("{\"last_updated_at\":1444883340000}");
    });

    it("should fetch latest version info if can update", async () => {
      jasmine.Ajax.stubRequest("/go/api/version_infos/stale", undefined, "GET").andReturn({
        responseText: JSON.stringify({update_server_url: "update.server.url"}),
        status: 200
      });

      jasmine.Ajax.stubRequest("update.server.url", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/go_server", undefined, "PATCH").andReturn({
        responseText: JSON.stringify({}),
        responseHeaders: {
          "Content-Type": `application/vnd.go.cd.v1+json`
        },
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/latest_version", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      await VersionUpdater.update();
    });

    it("should post the latest version info to server", async () => {
      jasmine.Ajax.stubRequest("/go/api/version_infos/stale", undefined, "GET").andReturn({
        responseText: JSON.stringify({update_server_url: "update.server.url"}),
        responseHeaders: {
          "Content-Type": `application/vnd.go.cd.v1+json`,
        },
        status: 200
      });

      jasmine.Ajax.stubRequest("update.server.url", undefined, "GET").andReturn({
        responseText: JSON.stringify({foo: "bar"}),
        responseHeaders: {
          "Content-Type": "application/json"
        },
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/go_server", undefined, "PATCH").andReturn({
        responseText: JSON.stringify({}),
        responseHeaders: {
          "Content-Type": `application/vnd.go.cd.v1+json`
        },
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/latest_version", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      mockDate.set("2015-10-15T04:29:00.000Z");

      await VersionUpdater.update();

      const updateServerRequest = jasmine.Ajax.requests.at(1);

      expect(updateServerRequest.method).toBe("GET");
      expect(updateServerRequest.url).toBe("update.server.url");
      expect(updateServerRequest.requestHeaders.Accept).toContain("application/vnd.update.go.cd.v1+json");

      const updateLatestRequest = jasmine.Ajax.requests.at(2);

      expect(updateLatestRequest.method).toBe("PATCH");
      expect(updateLatestRequest.url).toBe("/go/api/version_infos/go_server");
      expect(JSON.parse(updateLatestRequest.params)).toEqual({foo: "bar"});
      expect(updateLatestRequest.requestHeaders["Content-Type"]).toContain("application/json");
      expect(updateLatestRequest.requestHeaders.Accept).toContain("application/vnd.go.cd.v1+json");
      expect(localStorage.getItem("versionCheckInfo")).toEqual("{\"last_updated_at\":1444883340000}");
    });

    it("should post update check message in local storage if an update is available", async () => {
      jasmine.Ajax.stubRequest("/go/api/version_infos/stale", undefined, "GET").andReturn({
        responseText: JSON.stringify({update_server_url: "update.server.url"}),
        status: 200
      });

      jasmine.Ajax.stubRequest("update.server.url", undefined, "GET").andReturn({
        responseText: JSON.stringify({}),
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/go_server", undefined, "PATCH").andReturn({
        responseText: JSON.stringify({}),
        responseHeaders: {
          "Content-Type": `application/vnd.go.cd.v1+json`
        },
        status: 200
      });

      jasmine.Ajax.stubRequest("/go/api/version_infos/latest_version", undefined, "GET").andReturn({
        responseText: JSON.stringify({latest_version: "18.7.0-1234"}),
        status: 200
      });

      await VersionUpdater.update();

      const request = jasmine.Ajax.requests.at(3);
      expect(request.method).toBe("GET");
      expect(request.url).toBe("/go/api/version_infos/latest_version");
      expect(request.requestHeaders.Accept).toContain("application/vnd.go.cd.v1+json");
      const notifications = JSON.parse(localStorage.getItem("system_notifications") as string);
      expect(notifications.length).toBe(1);
      expect(notifications[0].message).toBe("A new version of GoCD - 18.7.0-1234 is available.");
      expect(notifications[0].type).toBe("UpdateCheck");
      expect(notifications[0].link).toBe("https://www.gocd.org/download/");
      expect(notifications[0].linkText).toBe("Learn More");
      expect(notifications[0].read).toBe(false);
      expect(notifications[0].id).not.toBeUndefined();
    });
  });

  const updateNotificationJSON = {
    id: "some-uuid",
    read: false,
    message: "A new version of GoCD - 19.3.0-1234 is available",
    type: "UpdateCheck",
    link: "/link/to/nowhere",
    linkText: "Click here to go nowhere"
  };
});
