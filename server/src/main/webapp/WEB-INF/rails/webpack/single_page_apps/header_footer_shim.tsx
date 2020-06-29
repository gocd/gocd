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

import "foundation-sites";
import $ from "jquery";
import m from "mithril";
import {footerMeta, headerMeta} from "models/current_user_permissions";
import {VersionUpdater} from "models/shared/version_updater";
import {ModalManager} from "views/components/modal/modal_manager";
import {SiteFooter} from "views/pages/partials/site_footer";
import {SiteHeader} from "views/pages/partials/site_header";

$(() => {
  window.addEventListener("DOMContentLoaded", () => {
    $(document).foundation();
    ModalManager.onPageLoad();
    VersionUpdater.update();

    const footerData = footerMeta();
    const headerData = headerMeta();

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
