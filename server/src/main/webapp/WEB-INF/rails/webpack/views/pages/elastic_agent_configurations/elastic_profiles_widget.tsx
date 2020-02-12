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
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as Icons from "views/components/icons";
import {IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

interface ElasticAgentProfileAddOperation {
  onAdd: (elasticAgentProfile: ElasticAgentProfile, e: MouseEvent) => void;
}

export type ElasticAgentOperations =
  EditOperation<ElasticAgentProfile>
  & DeleteOperation<string>
  & CloneOperation<ElasticAgentProfile>
  & ElasticAgentProfileAddOperation;

export interface Attrs {
  elasticProfiles: ElasticAgentProfiles;
  isUserAnAdmin: boolean;
  onShowUsages: (profileId: string, event: MouseEvent) => void;
  elasticAgentOperations: ElasticAgentOperations;
}

interface PluginInfoAttrs {
  pluginInfo?: PluginInfo;
}

export class ElasticProfilesWidget extends MithrilComponent<Attrs & PluginInfoAttrs, {}> {

  view(vnode: m.Vnode<Attrs & PluginInfoAttrs, {}>) {
    if (vnode.attrs.pluginInfo !== undefined && ElasticProfilesWidget.noElasticProfileConfigured(vnode)) {
      return <FlashMessage type={MessageType.info}
                           message="Click on 'Add' button to create new elastic agent profile."/>;
    }

    return (
      <div data-test-id="elastic-profile-list-parent">
        <div data-test-id="elastic-profile-list">
          {
            _.entries(vnode.attrs.elasticProfiles.groupByPlugin()).map(([pluginId, profiles]) => {
              return profiles.map((profile: ElasticAgentProfile) =>
                                    <ElasticProfileWidget key={profile.id()} elasticProfile={profile}
                                                          pluginInfo={vnode.attrs.pluginInfo}
                                                          onEdit={vnode.attrs.elasticAgentOperations.onEdit.bind(vnode.attrs,
                                                                                                                 profile)}
                                                          onClone={vnode.attrs.elasticAgentOperations.onClone.bind(vnode.attrs,
                                                                                                                   profile)}
                                                          onDelete={vnode.attrs.elasticAgentOperations.onDelete.bind(
                                                            vnode.attrs,
                                                            profile.id()!)}
                                                          onShowUsage={vnode.attrs.onShowUsages.bind(vnode.attrs,
                                                                                                     profile.id()!)}
                                    />
              );
            })
          }
        </div>
      </div>
    );
  }

  private static noElasticProfileConfigured(vnode: m.Vnode<Attrs & PluginInfoAttrs, {}>) {
    return vnode.attrs.elasticProfiles == null || vnode.attrs.elasticProfiles.empty();
  }
}

export interface ProfileAttrs {
  pluginInfo?: PluginInfo;
  elasticProfile: ElasticAgentProfile;
  onEdit: (e: MouseEvent) => void;
  onClone: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  onShowUsage: (e: MouseEvent) => void;
}

export class ElasticProfileWidget extends MithrilComponent<ProfileAttrs> {

  static profileHeader(profileId: string) {
    const title = <div data-test-id="elastic-profile-id"><span>{profileId}</span></div>;
    return <KeyValueTitle image={null} title={title}/>;
  }

  view(vnode: m.Vnode<ProfileAttrs, {}>) {
    const elasticProfile = vnode.attrs.elasticProfile;

    let isDisabled = false, disabledReason;

    if (!vnode.attrs.pluginInfo) {
      isDisabled     = true;
      disabledReason = `Could not find plugin with id ${vnode.attrs.elasticProfile.pluginId()}`;
    }

    if (!vnode.attrs.elasticProfile.canAdminister()) {
      isDisabled     = true;
      disabledReason = `You dont have permissions to administer '${vnode.attrs.elasticProfile.id()}' elastic agent profile.`;
    }

    const actions = [
      <IconGroup>
        <Icons.Edit data-test-id="edit-elastic-profile"
                    onclick={vnode.attrs.onEdit}
                    title={disabledReason}
                    disabled={isDisabled}/>
        <Icons.Clone data-test-id="clone-elastic-profile"
                     onclick={vnode.attrs.onClone}
                     disabled={!vnode.attrs.pluginInfo}/>
        <Icons.Delete data-test-id="delete-elastic-profile"
                      title={disabledReason}
                      disabled={isDisabled}
                      onclick={vnode.attrs.onDelete}/>
        <Icons.Usage data-test-id="show-usage-elastic-profile"
                     onclick={vnode.attrs.onShowUsage}/>
      </IconGroup>
    ];
    return (
      <CollapsiblePanel header={ElasticProfileWidget.profileHeader(elasticProfile.id()!)} actions={actions}
                        dataTestId={"elastic-profile-header"}>
        <KeyValuePair data={new Map(elasticProfile.properties() != null ? elasticProfile.properties()!.asMap() : [])}/>
      </CollapsiblePanel>
    );
  }
}
