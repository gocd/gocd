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
import $ from "jquery";
import _ from "lodash";
import {SystemNotifications} from "models/notifications/system_notifications";

import {mrequest} from "helpers/mrequest";
import {SparkRoutes} from "helpers/spark_routes";

interface VersionCheckInfo {
  last_updated_at: number;
}

interface StaleVersionInfo {
  component_name: string;
  update_server_url: string;
  installed_version: string;
  latest_version: string | null;
}

interface LatestVersionFromUpdateServer {
  message: string;
  message_signature: string;
  signing_public_key: string;
  signing_public_key_signature: string;
}

interface LatestVersion {
  latest_version?: string;
}

export class VersionUpdater {
  public static readonly CURRENT_GOCD_VERSION_KEY: string = "current-gocd-version";

  static async update() {
    const gocdVersionFromLocalStorage  = localStorage.getItem(this.CURRENT_GOCD_VERSION_KEY);
    const installedGoCDVersion: string = document.body.getAttribute("data-current-gocd-version")!;

    if (gocdVersionFromLocalStorage !== installedGoCDVersion) {
      localStorage.setItem(VersionUpdater.CURRENT_GOCD_VERSION_KEY, installedGoCDVersion);
      const notifications = await SystemNotifications.all();
      notifications.remove((notification) => (notification.type === "UpdateCheck"));
      SystemNotifications.setNotifications(notifications);
    }

    if (VersionUpdater.canUpdateVersion()) {
      const data = await VersionUpdater.fetchStaleVersionInfo();
        if (_.isEmpty(data)) {
          await VersionUpdater.markUpdateDoneAndNotify();
        } else {
          await VersionUpdater.fetchLatestVersion(data as StaleVersionInfo);
        }
    }
  }

  private static fetchStaleVersionInfo() {
    return $.ajax({
      method: "GET",
      url: SparkRoutes.staleVersionInfoPath(),
      beforeSend: mrequest.xhrConfig.forVersion("v1")
    });
  }

  private static canUpdateVersion(): boolean {
    const versionCheckInfoFromLocalStorage = localStorage.getItem("versionCheckInfo");

    if (_.isEmpty(versionCheckInfoFromLocalStorage)) {
      return true;
    }

    const versionCheckInfo: VersionCheckInfo = JSON.parse(versionCheckInfoFromLocalStorage as string);

    const lastUpdateAt: Date = new Date(versionCheckInfo.last_updated_at);
    const halfHourAgo: Date  = new Date(_.now() - 30 * 60 * 1000);

    return halfHourAgo > lastUpdateAt;
  }

  private static async markUpdateDoneAndNotify() {
    VersionUpdater.markUpdateDone();
    const latestVersionNumber = await VersionUpdater.get();
    if (latestVersionNumber !== undefined) {
      SystemNotifications.notifyNewMessage("UpdateCheck", `A new version of GoCD - ${latestVersionNumber} is available.`, "https://www.gocd.org/download/", "Learn More");
    }
  }

  private static async fetchLatestVersion(versionInfo: StaleVersionInfo) {
    const data = await $.ajax({
      method: "GET",
      url: versionInfo.update_server_url,
      beforeSend(xhr) {
        xhr.setRequestHeader("Accept", "application/vnd.update.go.cd.v1+json");
      },
    });
    await VersionUpdater.updateLatestVersion(data);
  }

  private static markUpdateDone() {
    const versionCheckInfo = JSON.stringify({last_updated_at: new Date().getTime()});
    localStorage.setItem("versionCheckInfo", versionCheckInfo);
  }

  private static async updateLatestVersion(data: LatestVersionFromUpdateServer) {
    await $.ajax({
      method: "PATCH",
      beforeSend: mrequest.xhrConfig.forVersion("v1"),
      url: SparkRoutes.updateServerVersionInfoPath(),
      data: JSON.stringify(data)
    });
    await VersionUpdater.markUpdateDoneAndNotify();
  }

  private static get() {
    return $.Deferred(function() {
      const deferred = this;

      const jqXHR = $.ajax({
        method: "GET",
        url: SparkRoutes.latestVersionInfoPath(),
        timeout: mrequest.timeout,
        beforeSend: mrequest.xhrConfig.forVersion("v1"),
        contentType: false
      });

      const didFulfill = (data: LatestVersion) => {
        deferred.resolve(data.latest_version);
      };

      const didReject = () => {
        deferred.reject(null);
      };

      jqXHR.then(didFulfill, didReject);
    }).promise();
  }
}
