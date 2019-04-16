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

import {MithrilComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {CollapsiblePanel} from "views/components/collapsible_panel";
import {FlashMessage, MessageType} from "views/components/flash_message";
import * as Icons from "views/components/icons";
import {IconGroup} from "views/components/icons";
import {KeyValuePair, KeyValueTitle} from "views/components/key_value_pair";
import {AddOperation, CloneOperation, DeleteOperation, EditOperation} from "views/pages/page_operations";

export type ElasticAgentOperations = EditOperation<ElasticAgentProfile> & DeleteOperation<string> & CloneOperation<ElasticAgentProfile> & AddOperation<void>;

export interface Attrs {
  pluginInfos: Stream<Array<PluginInfo<Extension>>>;
  elasticProfiles: ElasticAgentProfiles;
  isUserAnAdmin: boolean;
  onShowUsages: (profileId: string, event: MouseEvent) => void;
  elasticAgentOperations: ElasticAgentOperations;
}

export class ElasticProfilesWidget extends MithrilComponent<Attrs, {}> {

  view(vnode: m.Vnode<Attrs, {}>) {
    let noPluginInstalledMessage;

    if (ElasticProfilesWidget.noElasticAgentPluginInstalled(vnode)) {
      noPluginInstalledMessage = "No elastic agent plugin installed.";
    }

    if ((!noPluginInstalledMessage) && ElasticProfilesWidget.noElasticProfileConfigured(vnode)) {
      return <FlashMessage type={MessageType.info} message="Click on 'Add' button to create new elastic agent profile."/>;
    }

    return (
      <div data-test-id="elastic-profile-list-parent">
        <div data-test-id="elastic-profile-list">
          {
            _.entries(vnode.attrs.elasticProfiles.groupByPlugin()).map(([pluginId, profiles]) => {
              const pluginInfo = ElasticProfilesWidget.findPluginInfoByPluginId(vnode.attrs.pluginInfos(), pluginId);
              return profiles.map((profile: ElasticAgentProfile) =>
                                    <ElasticProfileWidget key={profile.id()} elasticProfile={profile}
                                                          pluginInfo={pluginInfo}
                                                          onEdit={vnode.attrs.elasticAgentOperations.onEdit.bind(vnode.attrs, profile)}
                                                          onClone={vnode.attrs.elasticAgentOperations.onClone.bind(vnode.attrs, profile)}
                                                          onDelete={vnode.attrs.elasticAgentOperations.onDelete.bind(vnode.attrs, profile.id())}
                                                          onShowUsage={vnode.attrs.onShowUsages.bind(vnode.attrs, profile.id())}
                                    />
              );
            })
          }
        </div>
      </div>
    );
  }

  private static findPluginInfoByPluginId(pluginInfos: Array<PluginInfo<Extension>>, pluginId: string) {
    return _.find(pluginInfos, ["id", pluginId]);
  }

  private static noElasticAgentPluginInstalled(vnode: m.Vnode<Attrs, {}>) {
    return vnode.attrs.pluginInfos() == null || vnode.attrs.pluginInfos().length === 0;
  }

  private static noElasticProfileConfigured(vnode: m.Vnode<Attrs, {}>) {
    return vnode.attrs.elasticProfiles == null || vnode.attrs.elasticProfiles.empty();
  }
}

export interface ProfileAttrs {
  pluginInfo: PluginInfo<Extension> | undefined;
  elasticProfile: ElasticAgentProfile;
  onEdit: (e: MouseEvent) => void;
  onClone: (e: MouseEvent) => void;
  onDelete: (e: MouseEvent) => void;
  onShowUsage: (e: MouseEvent) => void;
}

export class ElasticProfileWidget extends MithrilComponent<ProfileAttrs> {

  static profileHeader(profileId: string) {
    return <KeyValueTitle image={null} titleTestId="elastic-profile-id" title={profileId}/>;
  }

  view(vnode: m.Vnode<ProfileAttrs, {}>) {
    const elasticProfile = vnode.attrs.elasticProfile;
    const actions        = [
      <IconGroup>
        <Icons.Edit data-test-id="edit-elastic-profile" onclick={vnode.attrs.onEdit} disabled={!vnode.attrs.pluginInfo}/>
        <Icons.Clone data-test-id="clone-elastic-profile" onclick={vnode.attrs.onClone} disabled={!vnode.attrs.pluginInfo}/>
        <Icons.Delete data-test-id="delete-elastic-profile" onclick={vnode.attrs.onDelete}/>
        <Icons.Usage data-test-id="show-usage-elastic-profile" onclick={vnode.attrs.onShowUsage}/>
      </IconGroup>
    ];
    return (
      <CollapsiblePanel header={ElasticProfileWidget.profileHeader(elasticProfile.id())} actions={actions} dataTestId={"elastic-profile"}>
        <KeyValuePair data={new Map(elasticProfile.properties() != null ? elasticProfile.properties().asMap() : [])}/>
      </CollapsiblePanel>
    );
  }
}
