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

import classNames from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {PreferenceVM} from "models/new_preferences/preferences";
import style from "views/pages/agents/index.scss";
import {PreferencesState} from "views/pages/new_preferences";
import styles from "views/pages/server-configuration/index.scss";
import {EmailSettingsWidget} from "./email_settings_widget";
import {NotificationsWidget} from "./notifications_widget";

const classnames = classNames.bind(style);

export enum Sections {
  MY_NOTIFICATIONS = "my-notifications",
  EMAIL_SETTINGS   = "email-settings"
}

export class PreferencesWidget extends MithrilViewComponent<PreferencesState> {
  view(vnode: m.Vnode<PreferencesState>) {
    let activeConfigurationVM: PreferenceVM;
    switch (vnode.attrs.activeConfiguration) {
      case Sections.MY_NOTIFICATIONS:
        activeConfigurationVM = vnode.attrs.notificationVMs();
        break;
      case Sections.EMAIL_SETTINGS:
        activeConfigurationVM = vnode.attrs.currentUserVM();
        break;
    }
    return <div class={styles.serverConfigurationContainer}>
      <div class={styles.leftPanel}>
        <ul>
          <li data-test-id="my-notifications"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.MY_NOTIFICATIONS})}
              onclick={() => vnode.attrs.route(Sections.MY_NOTIFICATIONS, activeConfigurationVM)}>
            My Notifications
          </li>
          <li data-test-id="email-settings"
              class={classnames({[styles.active]: vnode.attrs.activeConfiguration === Sections.EMAIL_SETTINGS})}
              onclick={() => vnode.attrs.route(Sections.EMAIL_SETTINGS, activeConfigurationVM)}>
            Email Settings
          </li>
        </ul>
      </div>
      <div class={styles.rightPanel}>
        <PreferencesRightPanel {...vnode.attrs}/>
      </div>
    </div>;
  }
}

class PreferencesRightPanel extends MithrilViewComponent<PreferencesState> {
  view(vnode: m.Vnode<PreferencesState, this>): m.Children | void | null {
    switch (vnode.attrs.activeConfiguration) {
      case Sections.MY_NOTIFICATIONS:
        return <NotificationsWidget notificationVMs={vnode.attrs.notificationVMs}
                                    pipelineGroups={vnode.attrs.pipelineGroups}
                                    onAddFilter={vnode.attrs.onAddFilter}
                                    onEditFilter={vnode.attrs.onEditFilter}
                                    onDeleteFilter={vnode.attrs.onDeleteFilter}
                                    isSMTPConfigured={vnode.attrs.isSMTPConfigured}/>;
      case Sections.EMAIL_SETTINGS:
        return <EmailSettingsWidget currentUserVM={vnode.attrs.currentUserVM}
                                    onCancel={vnode.attrs.onCancel}
                                    onSaveEmailSettings={vnode.attrs.onSaveEmailSettings}
                                    isSMTPConfigured={vnode.attrs.isSMTPConfigured}/>;
    }
  }
}
