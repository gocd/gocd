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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {SecretConfig, SecretConfigs} from "models/secret_configs/secret_configs";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {isBlank} from "underscore.string";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {ShowRulesWidget} from "views/components/rules/show_rules_widget";
import {CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos} from "views/pages/page_operations";
import styles from "views/pages/secret_configs/index.scss";

interface Attrs extends RequiresPluginInfos, EditOperation<SecretConfig>, CloneOperation<SecretConfig>, DeleteOperation<SecretConfig> {
  secretConfigs: Stream<SecretConfigs>;
}

export class SecretConfigsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let noSecretPluginInstalled;
    if (!this.hasSecretPlugins(vnode)) {
      noSecretPluginInstalled =
        <FlashMessage type={MessageType.info} message="No secret plugin installed."/>;
    }
    if (vnode.attrs.secretConfigs === null || vnode.attrs.secretConfigs().length === 0) {
      return (<div class={styles.tips}>
        {noSecretPluginInstalled}
        <ul data-test-id="secret-config-info">
          <li>Click on "Add" to add new secret configuration.</li>
          <li>A secret configuration can be used to access secrets from a secret management store.</li>
        </ul>
      </div>);
    }

    return <div data-test-id="secret-configs">
      {noSecretPluginInstalled}
      {
        vnode.attrs.secretConfigs().map((secretConfig) => {
          const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: secretConfig().pluginId()});

          const header = [this.headerIcon(pluginInfo),
            <KeyValuePair inline={true} data={this.headerMap(secretConfig())}/>];

          const descriptionText = !isBlank(secretConfig().description()) ? <p data-test-id="secret-config-description">{secretConfig().description()}</p> : null;
          return <CollapsiblePanel
            dataTestId="secret-configs-group" header={header}
            actions={this.getActionButtons(vnode, secretConfig())}>
            {descriptionText}
            {this.infoFor(secretConfig())}
            <ShowRulesWidget rules={secretConfig().rules}/>
          </CollapsiblePanel>;
        })
      }
    </div>;
  }

  private infoFor(secretConfig: SecretConfig) {
    const properties = new Map(secretConfig.properties().asMap());
    return <KeyValuePair data={properties}/>;
  }

  private hasSecretPlugins(vnode: m.Vnode<Attrs>) {
    return vnode.attrs.pluginInfos && vnode.attrs.pluginInfos().length !== 0;
  }

  private getActionButtons(vnode: m.Vnode<Attrs>, secretConfig: SecretConfig) {
    return [
      <IconGroup>
        <Edit disabled={!this.hasSecretPlugins(vnode)}
              data-test-id="secret-config-edit"
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, secretConfig)}/>
        <Clone disabled={!this.hasSecretPlugins(vnode)}
               data-test-id="secret-config-clone"
               onclick={vnode.attrs.onClone.bind(vnode.attrs, secretConfig)}/>
        <Delete data-test-id="secret-config-delete"
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, secretConfig)}/>
      </IconGroup>
    ];
  }

  private headerMap(secretConfig: SecretConfig) {
    const map = new Map();
    map.set("Id", secretConfig.id());
    map.set("Plugin Id", secretConfig.pluginId());
    return map;
  }

  private headerIcon(pluginInfo?: PluginInfo) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }
}
