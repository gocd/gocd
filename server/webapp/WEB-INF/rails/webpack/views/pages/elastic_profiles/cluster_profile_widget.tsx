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
import {ClusterProfile} from "models/elastic_profiles/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {Attrs as ElasticProfilesWidgetAttrs, ElasticProfilesWidget} from "views/pages/elastic_profiles/elastic_profiles_widget";
import * as styles from "views/pages/elastic_profiles/index.scss";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

export type ClusterProfileOperations = EditOperation<ClusterProfile> & DeleteOperation<string> & AddOperation<void> & CloneOperation<ClusterProfile>;

export interface ClusterProfileWidgetAttrs extends ElasticProfilesWidgetAttrs {
  clusterProfile: Stream<ClusterProfile>;
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  isUserAnAdmin: boolean;
  clusterProfileOperations: ClusterProfileOperations;
}

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

export class ClusterProfileWidget extends MithrilComponent<ClusterProfileWidgetAttrs, {}> {
  view(vnode: m.Vnode<ClusterProfileWidgetAttrs, {}>) {
    const pluginInfo     = ClusterProfileWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(), vnode.attrs.clusterProfile().pluginId());
    const pluginImageTag = ClusterProfileWidget.createImageTag(pluginInfo);

    return <CollapsiblePanel key={vnode.attrs.clusterProfile().id()}
                             header={<ClusterProfilesHeaderWidget clusterProfileId={vnode.attrs.clusterProfile().id()} pluginId={vnode.attrs.clusterProfile().pluginId()} image={pluginImageTag}/>}
                             actions={this.getActionButtons(vnode)}
                             dataTestId={"cluster-profile-panel"}>
      {this.getClusterProfileDetails(vnode.attrs.clusterProfile())}
      <ElasticProfilesWidget elasticAgentProfiles={vnode.attrs.elasticAgentProfiles}
                             pluginInfos={vnode.attrs.pluginInfos}
                             elasticAgentOperations={vnode.attrs.elasticAgentOperations}
                             onShowUsages={vnode.attrs.onShowUsages.bind(vnode.attrs)}
                             isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
    </CollapsiblePanel>;
  }

  private static supportsClusterStatusReport(pluginInfo: PluginInfo<any> | undefined) {
    if (pluginInfo && pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS)) {
      const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
      return extension && extension.capabilities && extension.capabilities.supportsClusterStatusReport;
    }
    return false;
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

  private getActionButtons(vnode: m.Vnode<ClusterProfileWidgetAttrs>) {
    const pluginInfo    = ClusterProfileWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(), vnode.attrs.clusterProfile().pluginId());
    const actionButtons = [];

    if (pluginInfo != null && ClusterProfileWidget.supportsClusterStatusReport(pluginInfo)) {
      const statusReportPath: string = Routes.adminClusterStatusReportPath(vnode.attrs.clusterProfile().pluginId(), vnode.attrs.clusterProfile().id());

      actionButtons.push(
        <Buttons.Secondary onclick={this.goToStatusReportPage.bind(this, statusReportPath)}
                           data-test-id="status-report-link"
                           disabled={!vnode.attrs.isUserAnAdmin || !pluginInfo}>
          Status Report
        </Buttons.Secondary>);
    }

    actionButtons.push(
      <Buttons.Default onclick={vnode.attrs.elasticAgentOperations.onAdd.bind(vnode.state)} data-test-id={"new-elastic-agent-profile-button"} disabled={!pluginInfo}>
        + New Elastic Agent Profile
      </Buttons.Default>);

    actionButtons.push(<div className={styles.clusterProfileCrudActions}>
      <IconGroup>
        <Icons.Edit data-test-id="edit-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onEdit.bind(this, vnode.attrs.clusterProfile())} disabled={!pluginInfo}/>
        <Icons.Clone data-test-id="clone-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onClone.bind(this, vnode.attrs.clusterProfile())} disabled={!pluginInfo}/>
        <Icons.Delete data-test-id="delete-cluster-profile" onclick={vnode.attrs.clusterProfileOperations.onDelete.bind(this, vnode.attrs.clusterProfile().id())}/>
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

  private goToStatusReportPage(statusReportHref: string, event: Event): void {
    event.stopPropagation();
    window.location.href = statusReportHref;
  }
}
