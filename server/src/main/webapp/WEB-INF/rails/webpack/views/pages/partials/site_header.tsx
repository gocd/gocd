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
import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {NotificationCenter} from "views/components/notification_center";
import {ServerHealthSummary} from "views/components/server_health_summary/server_health_summary";
import {SiteMenu, SiteSubNavItem} from "views/components/site_menu";
import styles from "./site_header.scss";

const classnames          = bind(styles);

export interface Attrs {
  isAnonymous: boolean;
  userDisplayName: string;
  canViewTemplates: boolean;
  isGroupAdmin: boolean;
  isUserAdmin: boolean;
  canViewAdminPage: boolean;
  showAnalyticsDashboard: boolean;
  showMaterialsSpa: boolean;
}

export class SiteHeader extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const showAnalyticsDashboard = vnode.attrs.showAnalyticsDashboard;
    const canViewAdminPage       = vnode.attrs.canViewAdminPage;
    const isUserAdmin            = vnode.attrs.isUserAdmin;
    const isGroupAdmin           = vnode.attrs.isGroupAdmin;
    const canViewTemplates       = vnode.attrs.canViewTemplates;
    const userDisplayName        = vnode.attrs.userDisplayName;
    const isAnonymous            = vnode.attrs.isAnonymous;
    const showMaterialsSpa       = vnode.attrs.showMaterialsSpa;

    let userMenu: m.Children = null;

    if (!isAnonymous) {
      userMenu = (
        <div data-test-id="user-menu" class={classnames(styles.user, styles.isDropDown)}>
          <a data-test-id="username" href="#" class={styles.userLink}>
            <i class={styles.userIcon}/>
            {userDisplayName}
            <i class={`${styles.caretDownIcon}`}/>
          </a>

          <ul class={styles.userSubnav}>
            <SiteSubNavItem href="/go/preferences/notifications" text="Preferences"/>
            <SiteSubNavItem href="/go/access_tokens" text="Personal Access Tokens"/>
            <SiteSubNavItem href="/go/auth/logout" text="Sign out"/>
          </ul>
        </div>
      );
    }

    return (
      <header class={styles.siteHeader}>
        <a aria-label="GoCD Logo" href="/go/pipelines" class={styles.gocdLogo}/>
        <div class={styles.navbtn}>
          <div class={styles.bar}/>
        </div>
        <div class={styles.mainNavigation}>
          <div class={styles.siteHeaderLeft}>
            <SiteMenu showAnalytics={showAnalyticsDashboard}
                      canViewAdminPage={canViewAdminPage}
                      isUserAdmin={isUserAdmin}
                      isGroupAdmin={isGroupAdmin}
                      canViewTemplates={canViewTemplates}
                      showMaterialsSpa={showMaterialsSpa}
            />
          </div>

          <div class={styles.siteHeaderRight}>
            <NotificationCenter/>
            <ServerHealthSummary/>
            <a class={styles.needHelp} href="https://gocd.org/help" target="_blank">
              Need Help?
            </a>
            {userMenu}
          </div>
        </div>
      </header>
    );
  }
}
