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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {ArtifactStore, ArtifactStores} from "models/artifact_stores/artifact_stores_new";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {Clone, Delete, Edit, IconGroup} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation, RequiresPluginInfos} from "views/pages/page_operations";

interface Attrs extends RequiresPluginInfos, EditOperation<ArtifactStore>, CloneOperation<ArtifactStore>, DeleteOperation<ArtifactStore> {
  artifactStores: ArtifactStores;
}

export class ArtifactStoresWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    let noAuthorizationPluginMessage;
    if (!vnode.attrs.pluginInfos || vnode.attrs.pluginInfos().length === 0) {
      noAuthorizationPluginMessage =
        <FlashMessage type={MessageType.info} message="No artifact plugin installed."/>;
    }

    return <div data-test-id="auth-config-widget">
      {noAuthorizationPluginMessage}
      {vnode.attrs.artifactStores.map((artifactStore) => {
        const pluginInfo = _.find(vnode.attrs.pluginInfos(), {id: artifactStore.pluginId()});

        const header = [ArtifactStoresWidget.headerIcon(pluginInfo),
          <KeyValuePair inline={true} data={ArtifactStoresWidget.headerMap(artifactStore, pluginInfo)}/>];

        const actionButtons = [
          <IconGroup>
            <Edit data-test-id="auth-config-edit"
                  disabled={!pluginInfo}
                  onclick={vnode.attrs.onEdit.bind(vnode.attrs, artifactStore)}/>
            <Clone data-test-id="auth-config-clone"
                   disabled={!pluginInfo}
                   onclick={vnode.attrs.onClone.bind(vnode.attrs, artifactStore)}/>
            <Delete data-test-id="auth-config-delete"
                    onclick={vnode.attrs.onDelete.bind(vnode.attrs, artifactStore)}/>
          </IconGroup>];
        return <CollapsiblePanel header={header} actions={actionButtons}>
          <KeyValuePair data={artifactStore.properties().asMap()}/>
        </CollapsiblePanel>;
      })}
    </div>;
  }

  private static headerMap(artifactStore: ArtifactStore, pluginInfo?: PluginInfo<any>) {
    const map = new Map();
    map.set("Id", artifactStore.id());
    if (pluginInfo) {
      map.set("Plugin Id", pluginInfo.id);
    }
    return map;
  }

  private static headerIcon(pluginInfo?: PluginInfo<any>) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }
}
