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

import * as Routes from "gen/ts-routes";
import {MithrilComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ElasticProfile, ElasticProfiles} from "models/elastic_profiles/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentSettings, Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderIcon} from "views/components/header_icon";
import * as Icons from "views/components/icons";
import {ButtonGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import * as styles from "views/pages/elastic_profiles/index.scss";
import {CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

interface Attrs extends EditOperation<ElasticProfile>, DeleteOperation<string>, CloneOperation<ElasticProfile> {
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  elasticProfiles: ElasticProfiles;

  onShowUsages: (profileId: string, event: MouseEvent) => void;
}

interface HeaderAttrs {
  pluginName: string | undefined;
  pluginId: string;
  image: m.Children;
}

class ElasticProfilesHeaderWidget extends MithrilComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs, {}>) {
    return [
      (
        <KeyValueTitle title={ElasticProfilesHeaderWidget.createPluginNameElement(vnode.attrs.pluginName)}
                       image={vnode.attrs.image}/>
      ),
      (<KeyValuePair inline={true} data={new Map([
                                                   ["Plugin ID", vnode.attrs.pluginId]
                                                 ])}/>)
    ];
  }

  private static createPluginNameElement(pluginName: string | undefined) {
    if (pluginName) {
      return (<div data-test-id="plugin-name" className={styles.pluginName}>{pluginName}</div>);
    }

    return (<div className={styles.pluginNotInstalled}>Plugin is not installed</div>);
  }
}

export class ElasticProfilesWidget extends MithrilComponent<Attrs, {}> {

  view(vnode: m.Vnode<Attrs, {}>) {
    let noPluginInstalledMessage;

    if (ElasticProfilesWidget.noElasticAgentPluginInstalled(vnode)) {
      noPluginInstalledMessage = "No elastic agent plugin installed.";
    }

    if ((!noPluginInstalledMessage) && ElasticProfilesWidget.noElasticProfileConfigured(vnode)) {
      return <FlashMessage type={MessageType.info} message="Click on 'Add' button to create new elastic profile."/>;
    }

    return (
      <div data-test-id="elastic-profile-list-parent">
        <FlashMessage type={MessageType.info} message={noPluginInstalledMessage}/>
        <div data-test-id="elastic-profile-list">
          {
            _.entries(vnode.attrs.elasticProfiles.groupByPlugin()).map(([pluginId, profiles], index) => {
              const pluginInfo           = ElasticProfilesWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(),
                                                                                          pluginId);
              const pluginName           = pluginInfo ? pluginInfo.about.name : undefined;
              const statusReportButton   = this.createStatusReportButton(pluginId, pluginInfo);
              const pluginImageTag       = ElasticProfilesWidget.createImageTag(pluginInfo);
              const elasticProfileHeader = <ElasticProfilesHeaderWidget image={pluginImageTag}
                                                                        pluginId={pluginId}
                                                                        pluginName={pluginName}/>;
              return (
                <CollapsiblePanel key={pluginId} header={elasticProfileHeader} expanded={index === 0}
                                  actions={statusReportButton}>
                  {
                    profiles.map((profile: ElasticProfile) =>
                                   <ElasticProfileWidget key={profile.id()} elasticProfile={profile}
                                                         pluginInfo={pluginInfo}
                                                         onEdit={vnode.attrs.onEdit.bind(vnode.attrs, profile)}
                                                         onClone={vnode.attrs.onClone.bind(vnode.attrs, profile)}
                                                         onDelete={vnode.attrs.onDelete.bind(vnode.attrs, profile.id())}
                                                         onShowUsage={vnode.attrs.onShowUsages.bind(vnode.attrs,
                                                                                                    profile.id())}

                                   />
                    )
                  }
                </CollapsiblePanel>);
            })
          }
        </div>
      </div>
    );
  }

  private static noElasticAgentPluginInstalled(vnode: m.Vnode<Attrs, {}>) {
    return vnode.attrs.pluginInfos() == null || vnode.attrs.pluginInfos().length === 0;
  }

  private static noElasticProfileConfigured(vnode: m.Vnode<Attrs, {}>) {
    return vnode.attrs.elasticProfiles == null || vnode.attrs.elasticProfiles.empty();
  }

  private static findPluginInfoByPluginId(pluginInfos: Array<PluginInfo<Extension>>,
                                          pluginId: string) {
    return _.find(pluginInfos, ["id", pluginId]);
  }

  private static supportsStatusReport(pluginInfo: PluginInfo<Extension> | undefined) {
    if (!pluginInfo) {
      return false;
    }

    const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS) as ElasticAgentSettings;
    if (extension && extension.capabilities) {
      return extension.capabilities.supportsStatusReport;
    }
    return false;
  }

  private static goToStatusReportPage(statusReportHref: string, event: Event) {
    event.stopPropagation();
    window.location.href = statusReportHref;
  }

  private static createImageTag(pluginInfo: PluginInfo<any> | undefined) {
    if (pluginInfo && pluginInfo.imageUrl) {
      return <HeaderIcon name="Plugin Icon" imageUrl={pluginInfo.imageUrl}/>;
    }
    return <HeaderIcon/>;
  }

  private createStatusReportButton(pluginId: string, pluginInfo ?: PluginInfo<Extension>) {
    if (pluginInfo && ElasticProfilesWidget.supportsStatusReport(pluginInfo)) {
      const statusReportPath: string = Routes.adminStatusReportPath(pluginId);
      return (
        <Buttons.Secondary onclick={ElasticProfilesWidget.goToStatusReportPage.bind(this, statusReportPath)}
                           data-test-id="status-report-link"
                           icon={ButtonIcon.DOC}>Status Report
        </Buttons.Secondary>
      );
    }
  }
}

export interface ProfileAttrs {
  pluginInfo: PluginInfo<Extension> | undefined;
  elasticProfile: ElasticProfile;
  onEdit: (e: MouseEvent) => void;
  onClone: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  onShowUsage: (e: MouseEvent) => void;
}

export class ElasticProfileWidget extends MithrilComponent<ProfileAttrs> {

  static profileHeader(profileId: string) {
    return (<KeyValuePair inline={true} data={new Map([
                                                        ["Profile ID", profileId]
                                                      ])}/>);
  }

  view(vnode: m.Vnode<ProfileAttrs, {}>) {
    const elasticProfile = vnode.attrs.elasticProfile;
    const actions        = [
      <ButtonGroup>
        <Icons.Edit data-test-id="edit-elastic-profile" onclick={vnode.attrs.onEdit}
                    disabled={!vnode.attrs.pluginInfo}/>
        <Icons.Clone data-test-id="clone-elastic-profile" onclick={vnode.attrs.onClone}
                     disabled={!vnode.attrs.pluginInfo}/>
        <Icons.Delete data-test-id="delete-elastic-profile" onclick={vnode.attrs.onDelete}/>
        <Icons.Usage data-test-id="show-usage-elastic-profile" onclick={vnode.attrs.onShowUsage}/>
      </ButtonGroup>
    ];
    return (<CollapsiblePanel header={ElasticProfileWidget.profileHeader(elasticProfile.id())}
                              actions={actions}
                              dataTestId={"elastic-profile"}>
      <KeyValuePair data={elasticProfile.properties().asMap()}/>
    </CollapsiblePanel>);
  }
}
