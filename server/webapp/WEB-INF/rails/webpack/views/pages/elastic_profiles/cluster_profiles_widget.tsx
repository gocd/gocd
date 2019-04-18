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

import * as Routes from "gen/ts-routes";
import {MithrilComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ClusterProfile, ClusterProfiles, ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {Attrs as ElasticProfilesWidgetAttrs, ElasticProfilesWidget} from "views/pages/elastic_profiles/elastic_profiles_widget";
import * as styles from "views/pages/elastic_profiles/index.scss";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

export type ClusterProfileOperations = EditOperation<ClusterProfile> & DeleteOperation<string> & AddOperation<void> & CloneOperation<ClusterProfile>;

interface Attrs {
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  elasticProfiles: ElasticAgentProfiles;
  clusterProfiles: ClusterProfiles;
  isUserAnAdmin: boolean;
  clusterProfileOperations: ClusterProfileOperations;
}

export type ClusterProfilesWidgetAttrs = Attrs & ElasticProfilesWidgetAttrs;

interface HeaderAttrs {
  clusterProfileId: string;
  pluginId: string;
  image: m.Children;
}

class ClusterProfilesHeaderWidget extends MithrilComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, {}>) {
    const title = <div data-test-id="cluster-profile-name" className={styles.clusterProfileName}>{vnode.attrs.clusterProfileId}</div>;

    return [
      <KeyValueTitle title={title} image={vnode.attrs.image}/>,
      <KeyValuePair inline={true} data={new Map([["PluginId", vnode.attrs.pluginId]])}/>
    ];
  }
}

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
              const filteredElasticAgentProfiles = vnode.attrs.elasticProfiles.filterByClusterProfile(clusterProfile.id());
              const pluginInfo                   = ClusterProfilesWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(), clusterProfile.pluginId());
              const pluginImageTag               = ClusterProfilesWidget.createImageTag(pluginInfo);

              return <CollapsiblePanel key={clusterProfile.id()}
                                       header={<ClusterProfilesHeaderWidget clusterProfileId={clusterProfile.id()} pluginId={clusterProfile.pluginId()} image={pluginImageTag}/>}
                                       actions={this.getActionButtons(vnode, clusterProfile, pluginInfo)}
                                       dataTestId={"cluster-profile-panel"}>
                {this.getClusterProfileDetails(clusterProfile)}
                <ElasticProfilesWidget elasticProfiles={new ElasticAgentProfiles(filteredElasticAgentProfiles)}
                                       pluginInfos={vnode.attrs.pluginInfos}
                                       elasticAgentOperations={vnode.attrs.elasticAgentOperations}
                                       onShowUsages={vnode.attrs.onShowUsages.bind(vnode.attrs)}
                                       isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
              </CollapsiblePanel>;
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

  private static createImageTag(pluginInfo: PluginInfo<any> | undefined) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }

  private static findPluginInfoByPluginId(pluginInfos: Array<PluginInfo<Extension>>, pluginId: string) {
    return _.find(pluginInfos, ["id", pluginId]);
  }

  private static supportsClusterStatusReport(pluginInfo: PluginInfo<any> | undefined) {
    if (pluginInfo && pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS)) {
      const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
      return extension && extension.capabilities && extension.capabilities.supportsClusterStatusReport;
    }
    return false;
  }

  private goToStatusReportPage(statusReportHref: string, event: Event): void {
    event.stopPropagation();
    window.location.href = statusReportHref;
  }

  private getActionButtons(vnode: m.Vnode<ClusterProfilesWidgetAttrs>, clusterProfile: ClusterProfile, pluginInfo: PluginInfo<Extension> | undefined) {
    const actionButtons = [];

    if (pluginInfo != null && ClusterProfilesWidget.supportsClusterStatusReport(pluginInfo)) {
      const statusReportPath: string = Routes.adminClusterStatusReportPath(clusterProfile.pluginId(), clusterProfile.id());

      actionButtons.push(
        <Buttons.Secondary onclick={this.goToStatusReportPage.bind(this, statusReportPath)}
                           data-test-id="status-report-link"
                           disabled={!vnode.attrs.isUserAnAdmin || !pluginInfo}>
          Status Report
        </Buttons.Secondary>);
    }

    actionButtons.push(
      <Buttons.Default onclick={(e) => {
        vnode.attrs.elasticAgentOperations.onAdd(new ElasticAgentProfile("", clusterProfile.pluginId(), clusterProfile.id(), new Configurations([])), e);
      }} data-test-id={"new-elastic-agent-profile-button"} disabled={!pluginInfo}>
        + New Elastic Agent Profile
      </Buttons.Default>);

    actionButtons.push(<div className={styles.clusterProfileCrudActions}>
      <IconGroup>
        <Icons.Edit data-test-id="edit-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onEdit.bind(this, clusterProfile)} disabled={!pluginInfo}/>
        <Icons.Clone data-test-id="clone-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onClone.bind(this, clusterProfile)} disabled={!pluginInfo}/>
        <Icons.Delete data-test-id="delete-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onDelete.bind(this, clusterProfile.id())}/>
      </IconGroup>
    </div>);

    return actionButtons;
  }

  private getClusterProfileDetails(clusterProfile: ClusterProfile) {
    const clusterProfileProperties = clusterProfile.properties() ? clusterProfile.properties().asMap() : [];
    const clusterProfileDetails    = new Map([
                                               ["Id", clusterProfile.id()],
                                               ["PluginId", clusterProfile.pluginId()],
                                               ...Array.from(clusterProfileProperties)
                                             ]);
    return (
      <CollapsiblePanel key={"show-cluster-info"} header={"Show Cluster Info"} dataTestId={"cluster-profile-info-panel"}>
        <KeyValuePair data={clusterProfileDetails}/>
      </CollapsiblePanel>
    );
  }
}
