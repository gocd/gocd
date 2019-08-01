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
import * as m from "mithril";
import {VersionUpdater} from "models/shared/version_updater";
import * as styles from "./spa_base.scss";

import {MithrilViewComponent} from "jsx/mithril-component";
import {UsageDataReporter} from "models/shared/usage_data_reporter";
import {ModalManager} from "views/components/modal/modal_manager";
import {Attrs as SiteFooterAttrs, SiteFooter} from "views/pages/partials/site_footer";
import {Attrs as SiteHeaderAttrs, SiteHeader} from "views/pages/partials/site_header";

interface Attrs {
  showHeader: boolean;
  showFooter: boolean;
  headerData: SiteHeaderAttrs;
  footerData: SiteFooterAttrs;
}

class MainPage extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <div class={styles.page}>
        <div class={styles.pagewrap}>
          {this.showHeader(vnode)}
          <main class={styles.mainContainer}>
            {vnode.children}
          </main>
        </div>
        {this.showFooter(vnode)}
      </div>
    );
  }

  private showFooter(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.showFooter) {
      return <SiteFooter {...vnode.attrs.footerData} />;
    }
  }

  private showHeader(vnode: m.Vnode<Attrs>) {
    if (vnode.attrs.showHeader) {
      return <SiteHeader {...vnode.attrs.headerData}/>;
    }
  }
}

export default abstract class Page {
  private readonly pageToMount: any;

  constructor(pageToMount: any) {
    this.pageToMount = pageToMount;
    this.render();
  }

  extractBoolean(body: Element, attribute: string): boolean {
    return JSON.parse(body.getAttribute(attribute) as string);
  }

  protected showHeader() {
    return true;
  }

  protected showFooter() {
    return true;
  }

  protected enableUsageDataAndVersionUpdating() {
    return true;
  }

  private render() {
    const page = this;
    window.addEventListener("DOMContentLoaded", () => {

      if (page.enableUsageDataAndVersionUpdating()) {
        UsageDataReporter.report();
        VersionUpdater.update();
      }

      const body: Element = document.querySelector("body") as Element;

      const showAnalyticsDashboard     = this.extractBoolean(body, "data-show-analytics-dashboard");
      const canViewAdminPage           = this.extractBoolean(body, "data-can-user-view-admin");
      const isUserAdmin                = this.extractBoolean(body, "data-is-user-admin");
      const isGroupAdmin               = this.extractBoolean(body, "data-is-user-group-admin");
      const canViewTemplates           = this.extractBoolean(body, "data-can-user-view-templates");
      const isAnonymous                = this.extractBoolean(body, "data-user-anonymous");
      const isServerInMaintenanceMode  = this.extractBoolean(body, "data-is-server-in-maintenance-mode");
      const userDisplayName            = body.getAttribute("data-user-display-name") || "";
      const maintenanceModeUpdatedOn   = body.getAttribute("data-maintenance-mode-updated-on");
      const maintenanceModeUpdatedBy   = body.getAttribute("data-maintenance-mode-updated-by");

      const footerData = {
        maintenanceModeUpdatedOn,
        maintenanceModeUpdatedBy,
        isServerInMaintenanceMode,
        isSupportedBrowser: !/(MSIE|Trident)/i.test(navigator.userAgent)
      } as SiteFooterAttrs;

      const headerData = {
        showAnalyticsDashboard,
        canViewAdminPage,
        isUserAdmin,
        isGroupAdmin,
        canViewTemplates,
        userDisplayName,
        isAnonymous
      };

      m.mount(body, {
        view() {
          return (
            <MainPage headerData={headerData}
                      footerData={footerData}
                      showHeader={page.showHeader()}
                      showFooter={page.showFooter()}>
              {m(page.pageToMount as any)}
            </MainPage>
          );
        }
      });
      ModalManager.onPageLoad();
    });
  }
}
