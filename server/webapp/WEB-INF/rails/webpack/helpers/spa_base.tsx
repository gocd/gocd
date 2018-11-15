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

import * as m from "mithril";
import * as styles from "./spa_base.scss";

// import {UsageDataReporter} from "models/shared/usage_data_reporter";

import {MithrilViewComponent} from "jsx/mithril-component";
import {ModalManager} from "views/components/modal/modal_manager";
import {Attrs as SiteFooterAttrs} from "views/pages/partials/site_footer";
import {Attrs as SiteHeaderAttrs} from "views/pages/partials/site_header";

interface Attrs {
  headerData: SiteHeaderAttrs;
  footerData: SiteFooterAttrs;
}

class MainPage extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <main className={styles.mainContainer}>
        {vnode.children}
      </main>
    );

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

  private render() {
    const page = this;
    window.addEventListener("DOMContentLoaded", () => {
      // UsageDataReporter.report();

      const body: Element = document.querySelector("body") as Element;
      const componentMountPoint = document.querySelector(".component-mount-point") as Element;

      const copyrightYear    = body.getAttribute("data-version-copyright-year") as string;
      const goVersion        = body.getAttribute("data-version-go-version") as string;
      const fullVersion      = body.getAttribute("data-version-full") as string;
      const formattedVersion = body.getAttribute("data-version-formatted") as string;

      const showAnalyticsDashboard = this.extractBoolean(body, "data-show-analytics-dashboard");
      const showConfigRepos        = this.extractBoolean(body, "data-config-repo-enabled");
      const canViewAdminPage       = this.extractBoolean(body, "data-can-user-view-admin");
      const isUserAdmin            = this.extractBoolean(body, "data-is-user-admin");
      const isGroupAdmin           = this.extractBoolean(body, "data-is-user-group-admin");
      const canViewTemplates       = this.extractBoolean(body, "data-can-user-view-templates");
      const isAnonymous            = this.extractBoolean(body, "data-user-anonymous");
      const userDisplayName        = body.getAttribute("data-user-display-name") || "";

      const footerData = {
        copyrightYear, goVersion, fullVersion, formattedVersion
      };

      const headerData = {
        showAnalyticsDashboard,
        canViewAdminPage,
        isUserAdmin,
        isGroupAdmin,
        canViewTemplates,
        userDisplayName,
        isAnonymous,
        showConfigRepos
      };

      m.mount(componentMountPoint, {
        view() {
          return (
            <MainPage headerData={headerData} footerData={footerData}>
              {m(page.pageToMount as any)}
            </MainPage>
          );
        }
      });
      ModalManager.onPageLoad();
    });
  }
}
