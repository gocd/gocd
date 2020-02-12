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

import {docsUrl} from "gen/gocd_version";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {AuthConfig, AuthConfigs} from "models/auth_configs/auth_configs";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import {CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos} from "views/pages/page_operations";
import styles from "./index.scss";

interface Attrs extends RequiresPluginInfos, EditOperation<AuthConfig>, CloneOperation<AuthConfig>, DeleteOperation<AuthConfig> {
  authConfigs: AuthConfigs;
}

export class AuthConfigsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let noAuthorizationPluginMessage;
    if (!vnode.attrs.pluginInfos || vnode.attrs.pluginInfos().length === 0) {
      noAuthorizationPluginMessage =
        <FlashMessage type={MessageType.info} message="No authorization plugin installed."/>;
    }

    if (vnode.attrs.authConfigs == null || vnode.attrs.authConfigs.length === 0) {
      return (<div class={styles.tips}>
        {noAuthorizationPluginMessage}
        <ul data-test-id="auth-config-info">
          <li>Click on "Add" to add new authorization configuration.</li>
          <li>An auth configuration can be used to setup user authorization. You can read more about authorization in
            GoCD from <Link target="_blank" href={docsUrl("configuration/dev_authentication.html")}>here</Link>.
          </li>
        </ul>
      </div>);
    }

    return <div data-test-id="auth-config-widget">
      {noAuthorizationPluginMessage}
      {vnode.attrs.authConfigs.map((authConfig) => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: authConfig.pluginId()});

        const header = [this.headerIcon(pluginInfo),
          <KeyValuePair inline={true} data={this.headerMap(authConfig, pluginInfo)}/>];

        const actionButtons = [
          <IconGroup>
            <Edit data-test-id="auth-config-edit"
                  disabled={!pluginInfo}
                  onclick={vnode.attrs.onEdit.bind(vnode.attrs, authConfig)}/>
            <Clone data-test-id="auth-config-clone"
                   disabled={!pluginInfo}
                   onclick={vnode.attrs.onClone.bind(vnode.attrs, authConfig)}/>
            <Delete data-test-id="auth-config-delete"
                    onclick={vnode.attrs.onDelete.bind(vnode.attrs, authConfig)}/>
          </IconGroup>];
        return <CollapsiblePanel header={header} actions={actionButtons}>
          <KeyValuePair data={authConfig.properties()!.asMap()}/>
        </CollapsiblePanel>;
      })}
    </div>;
  }

  private headerMap(authConfig: AuthConfig, pluginInfo?: PluginInfo) {
    const map = new Map();
    map.set("Id", authConfig.id());
    if (pluginInfo) {
      map.set("Plugin Id", pluginInfo.id);
    }
    return map;
  }

  private headerIcon(pluginInfo?: PluginInfo) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }
}
