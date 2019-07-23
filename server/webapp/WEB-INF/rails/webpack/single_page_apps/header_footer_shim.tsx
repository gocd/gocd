/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import * as $ from "jquery";
import * as m from "mithril";
import {UsageDataReporter} from "models/shared/usage_data_reporter";
import {VersionUpdater} from "models/shared/version_updater";
import {ModalManager} from "views/components/modal/modal_manager";
import {SiteFooter} from "views/pages/partials/site_footer";
import {Attrs, SiteHeader} from "views/pages/partials/site_header";

require("foundation-sites");

$(() => {
  window.addEventListener("DOMContentLoaded", () => {
    // @ts-ignore
    $(document).foundation();
    ModalManager.onPageLoad();
    UsageDataReporter.report();
    VersionUpdater.update();

    const body = document.querySelector("body") as Element;

    function extractBoolean(body: Element, attribute: string): boolean {
      return JSON.parse(body.getAttribute(attribute) as string);
    }

    const showAnalyticsDashboard     = extractBoolean(body, "data-show-analytics-dashboard");
    const canViewAdminPage           = extractBoolean(body, "data-can-user-view-admin");
    const isUserAdmin                = extractBoolean(body, "data-is-user-admin");
    const isGroupAdmin               = extractBoolean(body, "data-is-user-group-admin");
    const canViewTemplates           = extractBoolean(body, "data-can-user-view-templates");
    const isAnonymous                = extractBoolean(body, "data-user-anonymous");
    const isServerInMaintenanceMode  = extractBoolean(body, "data-is-server-in-maintenance-mode");
    const userDisplayName            = body.getAttribute("data-user-display-name") || "";
    const maintenanceModeUpdatedOn   = body.getAttribute("data-maintenance-mode-updated-on");
    const maintenanceModeUpdatedBy   = body.getAttribute("data-maintenance-mode-updated-by");

    const footerData = {
      maintenanceModeUpdatedOn,
      maintenanceModeUpdatedBy,
      isServerInMaintenanceMode,
      isSupportedBrowser: !/(MSIE|Trident)/i.test(navigator.userAgent)
    };

    const headerData = {
      showAnalyticsDashboard,
      canViewAdminPage,
      isUserAdmin,
      isGroupAdmin,
      canViewTemplates,
      userDisplayName,
      isAnonymous
    } as Attrs;

    const menuMountPoint = document.querySelector("#app-menu");
    if (menuMountPoint) {
      const component = {
        view() {
          return (<SiteHeader {...headerData}/>);
        }
      };
      m.mount(menuMountPoint, component);
    } else {
      throw Error("Could not find menu mount point");
    }

    const footerMountPoint = document.querySelector("#app-footer");
    if (footerMountPoint) {
      const component = {
        view() {
          return (<SiteFooter {...footerData}/>);
        }
      };
      m.mount(footerMountPoint, component);
    } else {
      throw Error("Could not find footer mount point");
    }
  });
});
