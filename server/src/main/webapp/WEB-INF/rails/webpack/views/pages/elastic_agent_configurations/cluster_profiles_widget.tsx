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
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfiles} from "models/elastic_profiles/types";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {ClusterProfileWidget} from "views/pages/elastic_agent_configurations/cluster_profile_widget";
import {Attrs as ClusterProfileWidgetAttrs} from "views/pages/elastic_agent_configurations/cluster_profile_widget";

interface Attrs {
  pluginInfos: Stream<PluginInfos>;
  clusterProfiles: ClusterProfiles;
}

export type ClusterProfilesWidgetAttrs = Attrs & ClusterProfileWidgetAttrs;

export class ClusterProfilesWidget extends MithrilComponent<ClusterProfilesWidgetAttrs, {}> {

  view(vnode: m.Vnode<ClusterProfilesWidgetAttrs, {}>) {
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
            vnode.attrs.clusterProfiles.all().map((clusterProfile) => {
              return <ClusterProfileWidget {...vnode.attrs} clusterProfile={clusterProfile}/>;
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
    return vnode.attrs.clusterProfiles == null || vnode.attrs.clusterProfiles.all().length === 0;
  }
}
