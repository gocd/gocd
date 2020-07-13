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

import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores";
import {ArtifactExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import {NoPluginsOfTypeInstalled} from "views/components/no_plugins_installed";
import styles from "views/pages/elastic_agent_configurations/index.scss";
import {CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos} from "views/pages/page_operations";

interface Attrs extends RequiresPluginInfos, EditOperation<ArtifactStore>, CloneOperation<ArtifactStore>, DeleteOperation<ArtifactStore> {
  artifactStores: ArtifactStores;
}

export class ArtifactStoresWidget extends MithrilViewComponent<Attrs> {
  public static helpText() {
    return <ul>
      <li>Use this page to add a global artifact store to publish/fetch external artifacts.</li>
      <li>By default, GoCD internally manages and stores artifacts. The Artifact extension allows GoCD to make use of an external artifact store for
        storing artifacts while retaining traceability between pipelines.
      </li>
      <li>To build your own custom plugin, please refer <Link href={"https://plugin-api.gocd.org/current/artifacts/#artifact-plugins"}>this</Link>.
      </li>
    </ul>;
  }

  view(vnode: m.Vnode<Attrs>) {
    let noArtifactStorePluginMessage;
    if (!vnode.attrs.pluginInfos || vnode.attrs.pluginInfos().length === 0) {
      noArtifactStorePluginMessage = <NoPluginsOfTypeInstalled extensionType={new ArtifactExtensionType()}/>;
    }

    if (_.isEmpty(vnode.attrs.artifactStores)) {
      return <div data-test-id="artifact-stores-widget">
        {noArtifactStorePluginMessage}
        <div className={styles.tips}>
          {ArtifactStoresWidget.helpText()}
        </div>
      </div>;
    }

    return <div data-test-id="artifact-stores-widget">
      {noArtifactStorePluginMessage}
      {_.entries(vnode.attrs.artifactStores.groupByPlugin()).map(([pluginId, artifactStores], index) => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: pluginId});
        return (<CollapsiblePanel header={ArtifactStoresWidget.groupHeader(pluginId, pluginInfo)}
                                  dataTestId="artifact-stores-group">
          {artifactStores.map((artifactStore) => {
            return ArtifactStoresWidget.viewForArtifactStore(vnode, artifactStore, pluginInfo);
          })}
        </CollapsiblePanel>);
      })}
    </div>;
  }

  private static viewForArtifactStore(vnode: m.Vnode<Attrs>,
                                      artifactStore: ArtifactStore,
                                      pluginInfo?: PluginInfo) {
    const header = [
      <KeyValuePair inline={true} data={new Map([["Id", artifactStore.id()]])}/>];
    return <CollapsiblePanel header={header}
                             actions={ArtifactStoresWidget.getActionButtons(vnode, artifactStore, pluginInfo)}>
      <KeyValuePair data={artifactStore.properties().asMap()}/>
    </CollapsiblePanel>;
  }

  private static getActionButtons(vnode: m.Vnode<Attrs>,
                                  artifactStore: ArtifactStore,
                                  pluginInfo?: PluginInfo) {
    return [
      <IconGroup>
        <Edit data-test-id="artifact-store-edit"
              disabled={!pluginInfo}
              onclick={vnode.attrs.onEdit.bind(vnode.attrs, artifactStore)}/>
        <Clone data-test-id="artifact-store-clone"
               disabled={!pluginInfo}
               onclick={vnode.attrs.onClone.bind(vnode.attrs, artifactStore)}/>
        <Delete data-test-id="artifact-store-delete"
                onclick={vnode.attrs.onDelete.bind(vnode.attrs, artifactStore)}/>
      </IconGroup>];
  }

  private static groupHeader(pluginId: string, pluginInfo?: PluginInfo) {
    return [
      <KeyValueTitle title={ArtifactStoresWidget.createPluginNameElement(pluginInfo)}
                     image={ArtifactStoresWidget.headerIcon(pluginInfo)}/>,
      < KeyValuePair inline={true} data={new Map([["Plugin Id", pluginId]])}/>
    ];
  }

  private static createPluginNameElement(pluginInfo?: PluginInfo) {
    if (pluginInfo) {
      return (<div data-test-id="plugin-name" class={styles.pluginName}>{pluginInfo!.about.name}</div>);
    }

    return (<div data-test-id="plugin-name" class={styles.pluginNotInstalled}>Plugin is not installed</div>);
  }

  private static headerIcon(pluginInfo?: PluginInfo) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }
}
