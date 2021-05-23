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
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfile, ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ElasticAgentExtension} from "models/shared/plugin_infos_new/extensions";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {
  Attrs as ElasticProfilesWidgetAttrs,
  ElasticProfilesWidget
} from "views/pages/elastic_agent_configurations/elastic_profiles_widget";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";
import styles from ".//index.scss";

const classnames = classNames.bind(styles);
export type ClusterProfileOperations = EditOperation<ClusterProfile> & DeleteOperation<string> & AddOperation<void> & CloneOperation<ClusterProfile>;

export interface Attrs extends ElasticProfilesWidgetAttrs {
  pluginInfos: Stream<PluginInfos>;
  elasticProfiles: ElasticAgentProfiles;
  isUserAnAdmin: boolean;
  clusterProfileOperations: ClusterProfileOperations;
}

interface ClusterProfileAttrs {
  clusterProfile: ClusterProfile;
}

interface State {
  clusterProfileDetailsExpanded: Stream<boolean>;
}

type ClusterProfileWidgetAttrs = Attrs & ClusterProfileAttrs;

interface HeaderAttrs {
  clusterProfileId: string;
  pluginName: string;
  image: m.Children;
}

class ClusterProfileHeaderWidget extends MithrilComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, State>) {
    const title = <div data-test-id="cluster-profile-name" class={styles.clusterProfileName}><span>{vnode.attrs.clusterProfileId}</span></div>;
    return (<KeyValueTitle title={title} image={vnode.attrs.image}/>);
  }
}

const flag: (val?: boolean) => Stream<boolean> = Stream;

export class ClusterProfileWidget extends MithrilComponent<ClusterProfileWidgetAttrs, State> {
  oninit(vnode: m.Vnode<ClusterProfileWidgetAttrs, State>) {
    vnode.state.clusterProfileDetailsExpanded = flag(false);
  }

  view(vnode: m.Vnode<ClusterProfileWidgetAttrs, State>) {
    const filteredElasticAgentProfiles = vnode.attrs.elasticProfiles.filterByClusterProfile(vnode.attrs.clusterProfile.id()!);
    const pluginInfo                   = this.pluginInfo(vnode);
    const pluginImageTag               = ClusterProfileWidget.createImageTag(pluginInfo);
    const pluginName                   = pluginInfo ? pluginInfo.about.name : "";
    return <CollapsiblePanel key={vnode.attrs.clusterProfile.id()}
                             header={<ClusterProfileHeaderWidget clusterProfileId={vnode.attrs.clusterProfile.id()!} pluginName={pluginName} image={pluginImageTag}/>}
                             actions={this.getActionButtons(vnode)}
                             dataTestId={"cluster-profile-panel"}>
      {this.getClusterProfileDetails(vnode)}
      <h4>Elastic Agent Profiles</h4>
      <ElasticProfilesWidget elasticProfiles={new ElasticAgentProfiles(filteredElasticAgentProfiles)}
                             pluginInfo={pluginInfo}
                             elasticAgentOperations={vnode.attrs.elasticAgentOperations}
                             onShowUsages={vnode.attrs.onShowUsages.bind(vnode.attrs)}
                             isUserAnAdmin={vnode.attrs.isUserAnAdmin}/>
    </CollapsiblePanel>;
  }

  private static supportsClusterStatusReport(pluginInfo?: PluginInfo) {
    if (pluginInfo && pluginInfo.extensionOfType(ExtensionTypeString.ELASTIC_AGENTS)) {
      const extension = pluginInfo.extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS);
      return extension && extension.capabilities && extension.capabilities.supportsClusterStatusReport;
    }
    return false;
  }

  private static createImageTag(pluginInfo?: PluginInfo) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }

  private goToStatusReportPage(statusReportHref: string, event: Event): void {
    event.stopPropagation();
    window.location.href = statusReportHref;
  }

  private getActionButtons(vnode: m.Vnode<ClusterProfileWidgetAttrs>) {
    const actionButtons = [];
    const pluginInfo    = this.pluginInfo(vnode);
    if (pluginInfo != null && ClusterProfileWidget.supportsClusterStatusReport(pluginInfo)) {
      const statusReportPath: string = SparkRoutes.clusterStatusReportPath(vnode.attrs.clusterProfile.pluginId()!, vnode.attrs.clusterProfile.id()!);

      actionButtons.push(
        <Buttons.Secondary onclick={this.goToStatusReportPage.bind(this, statusReportPath)}
                           data-test-id="status-report-link"
                           icon={ButtonIcon.DOC}
                           disabled={!pluginInfo}>
          Status Report
        </Buttons.Secondary>);
    }

    let isDisabled = false, disabledReason = "";
    if (!pluginInfo) {
      isDisabled     = true;
      disabledReason = `Could not find plugin with id ${vnode.attrs.clusterProfile.pluginId()}`;
    }

    if (!vnode.attrs.clusterProfile.canAdminister()) {
      isDisabled     = true;
      disabledReason = `You dont have permissions to administer '${vnode.attrs.clusterProfile.id()}' cluster profile.`;
    }

    actionButtons.push(
      <Buttons.Secondary onclick={(e: MouseEvent) => {
        vnode.attrs.elasticAgentOperations.onAdd(new ElasticAgentProfile("", vnode.attrs.clusterProfile.pluginId(), vnode.attrs.clusterProfile.id(), true, new Configurations([])), e);
      }}
                         data-test-id={"new-elastic-agent-profile-button"}
                         disabled={!pluginInfo}
                         title={disabledReason}
                         icon={ButtonIcon.ADD}>
        Elastic Agent Profile
      </Buttons.Secondary>);

    actionButtons.push(<div class={styles.clusterProfileCrudActions}>
      <IconGroup>
        <Icons.Edit data-test-id="edit-cluster-profile"
                    onclick={vnode.attrs.clusterProfileOperations.onEdit.bind(this, vnode.attrs.clusterProfile)}
                    title={disabledReason}
                    disabled={isDisabled}/>
        <Icons.Clone data-test-id="clone-cluster-profile"
                     onclick={vnode.attrs.clusterProfileOperations.onClone.bind(this, vnode.attrs.clusterProfile)}
                     disabled={!pluginInfo}/>
        <Icons.Delete data-test-id="delete-cluster-profile"
                      title={disabledReason}
                      onclick={vnode.attrs.clusterProfileOperations.onDelete.bind(this, vnode.attrs.clusterProfile.id()!)}
                      disabled={isDisabled}/>
      </IconGroup>
    </div>);

    return actionButtons;
  }

  private toggle(vnode: m.Vnode<ClusterProfileWidgetAttrs, State>) {
    vnode.state.clusterProfileDetailsExpanded(!vnode.state.clusterProfileDetailsExpanded());
  }

  private getClusterProfileDetails(vnode: m.Vnode<ClusterProfileWidgetAttrs, State>) {
    const clusterProfileProperties = vnode.attrs.clusterProfile.properties() ? vnode.attrs.clusterProfile.properties()!.asMap() : [];
    const clusterProfileDetails    = new Map([
                                               ["Id", vnode.attrs.clusterProfile.id()],
                                               ["PluginId", vnode.attrs.clusterProfile.pluginId()],
                                               ...Array.from(clusterProfileProperties)
                                             ]);
    return (
      <div class={styles.clusterProfileDetailsContainer}>
        <h5 class={classnames(styles.clusterProfileDetailsHeader, {[styles.expanded]: vnode.state.clusterProfileDetailsExpanded()})} onclick={this.toggle.bind(this, vnode)} data-test-id="cluster-profile-details-header">Cluster configuration</h5>
        <div class={classnames(styles.clusterProfileDetails, {[styles.expanded]: vnode.state.clusterProfileDetailsExpanded()})} data-test-id="cluster-profile-details">
          <KeyValuePair data={clusterProfileDetails}/>
        </div>
      </div>
    );
  }

  private pluginInfo(vnode: m.Vnode<ClusterProfileWidgetAttrs>) {
    return _.find(vnode.attrs.pluginInfos(), ["id", vnode.attrs.clusterProfile.pluginId()]);
  }
}
