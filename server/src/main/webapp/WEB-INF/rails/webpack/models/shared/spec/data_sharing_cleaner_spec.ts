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

import {SystemNotifications} from "models/notifications/system_notifications";
import {DataSharingCleaner} from "models/shared/data_sharing_cleaner";

describe("Data Sharing Cleaner", () => {
  beforeEach(() => {
    localStorage.clear();
  });

  afterEach(() => {
    localStorage.clear();
  });

  it("should clear data sharing last reported time from the local storage", () => {
    const time = "1234567890";
    const key  = DataSharingCleaner.USAGE_DATA_LAST_REPORTED_TIME_KEY;
    localStorage.setItem(key, time);

    expect(localStorage.getItem(key)).toBe(time);

    DataSharingCleaner.clean();

    expect(localStorage.getItem(key)).toBeFalsy("");
  });

  it("should not fail to clear data sharing last reported time when none exists in the local storage", () => {
    const key  = DataSharingCleaner.USAGE_DATA_LAST_REPORTED_TIME_KEY;
    expect(localStorage.getItem(key)).toBeFalsy("");

    DataSharingCleaner.clean();

    expect(localStorage.getItem(key)).toBeFalsy("");
  });

  it("should clear data sharing notification for version prior to GoCD v18.8.0", (done) => {
    const message = "GoCD shares data so that it can be improved.";
    const link    = "/go/admin/data_sharing/settings";
    const type    = "DataSharing";
    SystemNotifications.notifyNewMessage(type, message, link, "Learn more ...");

    expect(JSON.parse(localStorage.getItem("system_notifications")!)).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem("system_notifications")!)[0].type).toBe("DataSharing");

    DataSharingCleaner.clean().then(() => {
      expect(JSON.parse(localStorage.getItem("system_notifications")!)).toHaveLength(0);
      done();
    });
  });

  it("should clear data sharing notification for version next to GoCD v18.8.0", (done) => {
    const message = "GoCD’s shared data has been updated to include new metrics.";
    const link    = "/go/admin/data_sharing/settings";
    const type    = "DataSharing_v18.8.0";
    SystemNotifications.notifyNewMessage(type, message, link, "Learn more ...");

    expect(JSON.parse(localStorage.getItem("system_notifications")!)).toHaveLength(1);
    expect(JSON.parse(localStorage.getItem("system_notifications")!)[0].type).toBe("DataSharing_v18.8.0");

    DataSharingCleaner.clean().then(() => {
      expect(JSON.parse(localStorage.getItem("system_notifications")!)).toHaveLength(0);
      done();
    });
  });

  it("should not fail to clear data sharing notification when none exists", (done) => {
    expect(JSON.parse(localStorage.getItem("system_notifications") || "[]")).toHaveLength(0);

    DataSharingCleaner.clean().then(() => {
      expect(JSON.parse(localStorage.getItem("system_notifications") || "[]")).toHaveLength(0);
      done();
    });
  });

});
