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

import "foundation-sites";
import {flagAttr} from "helpers/dom";
import $ from "jquery";
import m from "mithril";
import {UsageDataReporter} from "models/shared/usage_data_reporter";
import {VersionUpdater} from "models/shared/version_updater";
import {ModalManager} from "views/components/modal/modal_manager";
import {Attrs as FooterAttrs, SiteFooter} from "views/pages/partials/site_footer";
import {Attrs as HeaderAttrs, SiteHeader} from "views/pages/partials/site_header";

$(() => {
  window.addEventListener("DOMContentLoaded", () => {
    $(document).foundation();
    ModalManager.onPageLoad();
    UsageDataReporter.report();
    VersionUpdater.update();

    const body = document.body;

    const showAnalyticsDashboard    = flagAttr(body, "data-show-analytics-dashboard");
    const canViewAdminPage          = flagAttr(body, "data-can-user-view-admin");
    const isUserAdmin               = flagAttr(body, "data-is-user-admin");
    const isGroupAdmin              = flagAttr(body, "data-is-user-group-admin");
    const canViewTemplates          = flagAttr(body, "data-can-user-view-templates");
    const isAnonymous               = flagAttr(body, "data-user-anonymous");
    const isServerInMaintenanceMode = flagAttr(body, "data-is-server-in-maintenance-mode");
    const userDisplayName           = body.getAttribute("data-user-display-name") || "";
    const maintenanceModeUpdatedOn  = body.getAttribute("data-maintenance-mode-updated-on");
    const maintenanceModeUpdatedBy  = body.getAttribute("data-maintenance-mode-updated-by");

    const footerData: FooterAttrs = {
      maintenanceModeUpdatedOn,
      maintenanceModeUpdatedBy,
      isServerInMaintenanceMode,
      isSupportedBrowser: !/(MSIE|Trident)/i.test(navigator.userAgent)
    };

    const headerData: HeaderAttrs = {
      showAnalyticsDashboard,
      canViewAdminPage,
      isUserAdmin,
      isGroupAdmin,
      canViewTemplates,
      userDisplayName,
      isAnonymous
    };

    const menuMountPoint = document.getElementById("app-menu");

    if (menuMountPoint) {
      m.mount(menuMountPoint, {
        view() {
          return <SiteHeader {...headerData}/>;
        }
      });
    } else {
      throw Error("Could not find menu mount point");
    }

    const footerMountPoint = document.getElementById("app-footer");

    if (footerMountPoint) {
      m.mount(footerMountPoint, {
        view() {
          return <SiteFooter {...footerData}/>;
        }
      });
    } else {
      throw Error("Could not find footer mount point");
    }
  });
});
