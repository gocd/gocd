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
import {HeaderIcon} from "views/components/header_icon";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import * as styles from "views/pages/cluster_profiles/index.scss";

interface ClusterProfilesWidgetAttrs {
  clusterProfiles: ClusterProfiles;
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
}

interface ClusterProfileWidgetAttrs {
  clusterProfile: ClusterProfile;
  pluginInfo: PluginInfo<Extension> | undefined;
}

interface HeaderAttrs {
  pluginName: string | undefined;
  pluginId: string | undefined;
  image: m.Children;
}

class ClusterProfileHeaderWidget extends MithrilComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, {}>) {
    return [
      <KeyValueTitle title={ClusterProfileHeaderWidget.createPluginNameElement(vnode.attrs.pluginName)} image={vnode.attrs.image}/>,
      <KeyValuePair inline={true} data={new Map([["Plugin Id", vnode.attrs.pluginId]])}/>
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
    const pluginName     = vnode.attrs.pluginInfo ? vnode.attrs.pluginInfo.about.name : undefined;
    const pluginImageTag = ClusterProfileWidget.createImageTag(vnode.attrs.pluginInfo);
    const pluginId       = vnode.attrs.pluginInfo ? vnode.attrs.pluginInfo.id : undefined;

    const clusterProfileHeader = <ClusterProfileHeaderWidget image={pluginImageTag}
                                                             pluginId={pluginId}
                                                             pluginName={pluginName}/>;

    return (
      <CollapsiblePanel key={pluginId} header={clusterProfileHeader}>
        <CollapsiblePanel header={ClusterProfileWidget.profileHeader(clusterProfile.id())}
                          dataTestId={"cluster-profile"}>
          <KeyValuePair data={clusterProfile.properties().asMap()}/>
        </CollapsiblePanel>
      </CollapsiblePanel>);
  }

  private static createImageTag(pluginInfo: PluginInfo<Extension> | undefined) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }
}

export class ClusterProfilesWidget extends MithrilViewComponent<ClusterProfilesWidgetAttrs> {
  view(vnode: m.Vnode<ClusterProfilesWidgetAttrs>) {
    return (
      <div data-test-id="cluster-profile-list-parent">
        <div data-test-id="cluster-profile-list">
          {
            _.entries(vnode.attrs.clusterProfiles.groupByPlugin()).map(([pluginId, profiles]) => {
              return profiles.map((clusterProfile: ClusterProfile) => <ClusterProfileWidget
                clusterProfile={clusterProfile} pluginInfo={ClusterProfilesWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(), pluginId)}/>);
            })
          }
        </div>
      </div>
    );
  }

  private static findPluginInfoByPluginId(pluginInfos: Array<PluginInfo<Extension>>, pluginId: any) {
    return _.find(pluginInfos, ["id", pluginId]);
  }
}
