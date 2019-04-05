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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ClusterProfile, ClusterProfiles} from "models/cluster_profiles/cluster_profiles";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import {IconGroup} from "views/components/icons";
import * as Icons from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import * as styles from "views/pages/cluster_profiles/index.scss";
import {DeleteOperation, EditOperation} from "views/pages/page_operations";

interface ClusterProfilesWidgetAttrs extends EditOperation<ClusterProfile>, DeleteOperation<string> {
  clusterProfiles: ClusterProfiles;
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
}

interface ClusterProfileWidgetAttrs {
  clusterProfile: ClusterProfile;
  onEdit: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
}

interface HeaderAttrs {
  pluginName: string | undefined;
  pluginId: string;
  image: m.Children;
}

class ClusterProfilesHeaderWidget extends MithrilComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, {}>) {
    return [
      (
        <KeyValueTitle title={ClusterProfilesHeaderWidget.createPluginNameElement(vnode.attrs.pluginName)}
                       image={vnode.attrs.image}/>
      ),
      (<KeyValuePair inline={true} data={new Map([
                                                   ["Plugin Id", vnode.attrs.pluginId]
                                                 ])}/>)
    ];
  }

  private static createPluginNameElement(pluginName: string | undefined) {
    return (<div data-test-id="plugin-name" className={styles.pluginName}>{pluginName}</div>);
  }
}

export class ClusterProfileWidget extends MithrilViewComponent<ClusterProfileWidgetAttrs> {
  static profileHeader(clusterProfileId: string) {
    return (<KeyValuePair inline={true} data={new Map([
                                                        ["Cluster Profile Id", clusterProfileId]
                                                      ])}/>);
  }

  view(vnode: m.Vnode<ClusterProfileWidgetAttrs, {}>) {
    const clusterProfile = vnode.attrs.clusterProfile;
    const actions        = [
      <IconGroup>
        <Icons.Edit data-test-id="edit-cluster-profile" onclick={vnode.attrs.onEdit}/>
        <Icons.Delete data-test-id="delete-cluster-profile" onclick={vnode.attrs.onDelete}/>
      </IconGroup>
    ];
    return (<CollapsiblePanel header={ClusterProfileWidget.profileHeader(clusterProfile.id())}
                              actions={actions}
                              dataTestId={"cluster-profile"}>
      <KeyValuePair data={clusterProfile.properties().asMap()}/>
    </CollapsiblePanel>);
  }
}

export class ClusterProfilesWidget extends MithrilViewComponent<ClusterProfilesWidgetAttrs> {
  view(vnode: m.Vnode<ClusterProfilesWidgetAttrs>) {

    let noPluginInstalledMessage;

    if (ClusterProfilesWidget.noElasticAgentPluginInstalled(vnode)) {
      noPluginInstalledMessage = "No elastic agent plugin installed.";
    }

    if ((!noPluginInstalledMessage) && ClusterProfilesWidget.noClusterProfileConfigured(vnode)) {
      return <FlashMessage type={MessageType.info} message="Click on 'Add' button to create new cluster profile."/>;
    }

    return (
      <div data-test-id="cluster-profile-list-parent">
        <FlashMessage type={MessageType.info} message={noPluginInstalledMessage}/>
        <div data-test-id="cluster-profile-list">
          {
            _.entries(vnode.attrs.clusterProfiles.groupByPlugin()).map(([pluginId, profiles]) => {
              const pluginInfo     = ClusterProfilesWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(),
                                                                                    pluginId);
              const pluginName     = pluginInfo ? pluginInfo.about.name : undefined;
              const pluginImageTag = ClusterProfilesWidget.createImageTag(pluginInfo);

              const clusterProfileHeader = <ClusterProfilesHeaderWidget image={pluginImageTag}
                                                                        pluginId={pluginId}
                                                                        pluginName={pluginName}/>;
              return (
                <CollapsiblePanel key={pluginId} header={clusterProfileHeader}>
                  {
                    profiles.map((clusterProfile: ClusterProfile) =>
                                   <ClusterProfileWidget clusterProfile={clusterProfile}
                                                         onEdit={vnode.attrs.onEdit.bind(vnode.attrs,
                                                                                         clusterProfile)}
                                                         onDelete={vnode.attrs.onDelete.bind(vnode.attrs,
                                                                                             clusterProfile.id())}
                                   />)
                  }
                </CollapsiblePanel>);
            })
          }
        </div>
      </div>
    );
  }

  private static noElasticAgentPluginInstalled(vnode: m.Vnode<ClusterProfilesWidgetAttrs, {}>) {
    return vnode.attrs.pluginInfos() == null || vnode.attrs.pluginInfos().length === 0;
  }

  private static noClusterProfileConfigured(vnode: m.Vnode<ClusterProfilesWidgetAttrs, {}>) {
    return vnode.attrs.clusterProfiles == null || vnode.attrs.clusterProfiles.length === 0;
  }

  private static createImageTag(pluginInfo: PluginInfo<any> | undefined) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }

  private static findPluginInfoByPluginId(pluginInfos: Array<PluginInfo<Extension>>, pluginId: any) {
    return _.find(pluginInfos, ["id", pluginId]);
  }
}
