/*
 * Copyright 2021 ThoughtWorks, Inc.
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

import {Attrs as SiteFooterAttrs} from "views/pages/partials/site_footer";
import {Attrs as SiteHeaderAttrs} from "views/pages/partials/site_header";

let _footerMeta: SiteFooterAttrs;
let _headerMeta: SiteHeaderAttrs;

export function footerMeta(): SiteFooterAttrs {
  if (!_footerMeta) {
    computeFooterMeta();
  }

  return _footerMeta;
}

export function headerMeta(): SiteHeaderAttrs {
  if (!_headerMeta) {
    computeHeaderMeta();
  }

  return _headerMeta;
}

function flagAttr(el: Element, attr: string): boolean {
  const value = el.getAttribute(attr);

  if (!value || !["true", "false"].includes(value)) {
    throw new TypeError(`attr [${attr}] on element ${el} must be either "true" or "false"; instead, was ${JSON.stringify(
      value)}`);
  }

  return "true" === value;
}

function computeFooterMeta() {
  const body                      = document.body;
  const isServerInMaintenanceMode = flagAttr(body, "data-is-server-in-maintenance-mode");
  const maintenanceModeUpdatedOn  = body.getAttribute("data-maintenance-mode-updated-on");
  const maintenanceModeUpdatedBy  = body.getAttribute("data-maintenance-mode-updated-by");

  _footerMeta = {
    maintenanceModeUpdatedOn,
    maintenanceModeUpdatedBy,
    isServerInMaintenanceMode,
    isSupportedBrowser: !/(MSIE|Trident)/i.test(navigator.userAgent)
  };
}

function computeHeaderMeta() {
  const body                   = document.body;
  const showAnalyticsDashboard = flagAttr(body, "data-show-analytics-dashboard");
  const canViewAdminPage       = flagAttr(body, "data-can-user-view-admin");
  const isUserAdmin            = flagAttr(body, "data-is-user-admin");
  const isGroupAdmin           = flagAttr(body, "data-is-user-group-admin");
  const canViewTemplates       = flagAttr(body, "data-can-user-view-templates");
  const isAnonymous            = flagAttr(body, "data-user-anonymous");
  const userDisplayName        = body.getAttribute("data-user-display-name") || "";

  _headerMeta = {
    showAnalyticsDashboard,
    canViewAdminPage,
    isUserAdmin,
    isGroupAdmin,
    canViewTemplates,
    userDisplayName,
    isAnonymous
  };
}
