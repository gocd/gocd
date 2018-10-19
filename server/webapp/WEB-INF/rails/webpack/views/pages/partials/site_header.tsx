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

import {MithrilComponent} from "../../../jsx/mithril-component";

import * as m from 'mithril';
import * as styles from './site_header.scss';

const classnames = require('classnames/bind').bind(styles);

const NotificationCenter  = require('views/components/notification_center/notification_center');
const ServerHealthSummary = require('views/components/server_health_summary/server_health_summary');
const SiteMenu            = require('views/components/site_menu/index');
const SiteHeaderLink      = require('views/components/site_header_link/index');

export interface Attrs {
  isAnonymous: boolean;
  userDisplayName: string;
  canViewTemplates: boolean;
  isGroupAdmin: boolean;
  isUserAdmin: boolean;
  canViewAdminPage: boolean;
  showAnalyticsDashboard: boolean;
}

export class SiteHeader extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const showAnalyticsDashboard = vnode.attrs.showAnalyticsDashboard;
    const canViewAdminPage       = vnode.attrs.canViewAdminPage;
    const isUserAdmin            = vnode.attrs.isUserAdmin;
    const isGroupAdmin           = vnode.attrs.isGroupAdmin;
    const canViewTemplates       = vnode.attrs.canViewTemplates;
    const userDisplayName        = vnode.attrs.userDisplayName;
    const isAnonymous            = vnode.attrs.isAnonymous;

    let userMenu: JSX.Element | null = null;

    if (!isAnonymous) {
      userMenu = (
        <div class={classnames(styles.user, styles.isDropDown)}>
          <SiteHeaderLink href="#" class={styles.userLink}>
            <i class={styles.userIcon}/>
            {userDisplayName}
            <i class={`${styles.caretDownIcon}`}/>
          </SiteHeaderLink>

          <ul class={styles.userSubnav}>
            <li>
              <a href="/go/preferences/notifications" class={styles.userSubnavLink}>Preference</a>
            </li>
            <li>
              <a href="/go/auth/logout" class={styles.userSubnavLink}>Sign out</a>
            </li>
          </ul>
        </div>
      );
    }

    return (
      <header class={styles.siteHeader}>
        <a href="/go/pipelines" class={styles.gocdLogo}/>
        <div class={styles.navbtn}>
          <div class={styles.bar}/>
        </div>
        <div class={styles.mainNavigation}>
          <div class={styles.siteHeaderLeft}>
            <SiteMenu showAnalytics={showAnalyticsDashboard}
                      canViewAdminPage={canViewAdminPage}
                      isUserAdmin={isUserAdmin}
                      isGroupAdmin={isGroupAdmin}
                      canViewTemplates={canViewTemplates}/>
          </div>

          <div class={styles.siteHeaderRight}>
            <NotificationCenter/>
            <ServerHealthSummary/>
            <SiteHeaderLink class={styles.needHelp} href="https://gocd.org/help" target="_blank">
              Need Help?
            </SiteHeaderLink>
            {userMenu}
          </div>
        </div>
      </header>
    );
  }
}

