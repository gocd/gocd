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

import {ErrorResponse} from "helpers/api_request_builder";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfilesCRUD} from "models/elastic_profiles/cluster_profiles_crud";
import {ElasticAgentProfilesCRUD} from "models/elastic_profiles/elastic_agent_profiles_crud";
import {
  ClusterProfile,
  ClusterProfiles,
  ElasticAgentProfile,
  ElasticAgentProfiles
} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {ElasticAgentsExtensionType, ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {NoPluginsOfTypeInstalled} from "views/components/no_plugins_installed";
import {CloseListener} from "views/components/wizard";
import {ElasticProfilesPage} from "views/pages/elastic_agent_configurations";
import {ClusterProfileOperations} from "views/pages/elastic_agent_configurations/cluster_profile_widget";
import {CloneClusterProfileModal} from "views/pages/elastic_agent_configurations/cluster_profiles_modals";
import {ClusterProfilesWidget} from "views/pages/elastic_agent_configurations/cluster_profiles_widget";
import {
  CloneElasticProfileModal,
  UsageElasticProfileModal
} from "views/pages/elastic_agent_configurations/elastic_agent_profiles_modals";
import {ElasticAgentOperations} from "views/pages/elastic_agent_configurations/elastic_profiles_widget";
import {HelpText} from "views/pages/elastic_agents/help_text";
import {
  openWizardForAdd,
  openWizardForAddElasticProfile,
  openWizardForEditClusterProfile,
  openWizardForEditElasticProfile
} from "views/pages/elastic_agents/wizard";
import {Page, PageState} from "views/pages/page";
import {RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";

export interface State extends RequiresPluginInfos, SaveOperation {
  onShowUsages: (profileId: string, event: MouseEvent) => void;
  elasticProfiles: Stream<ElasticAgentProfiles>;
  clusterProfiles: Stream<ClusterProfiles>;
  clusterProfileBeingEdited: Stream<ClusterProfile>;
  elasticProfileBeingEdited: Stream<ElasticAgentProfile>;
  elasticAgentOperations: ElasticAgentOperations;
  clusterProfileOperations: ClusterProfileOperations;
  isWizardOpen: Stream<boolean>;
  onClose: () => void;
}

export class ElasticAgentsPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    if (vnode.state.pluginInfos().length === 0) {
      return (<NoPluginsOfTypeInstalled extensionType={new ElasticAgentsExtensionType()}/>);
    }
    if (vnode.state.clusterProfiles().empty()) {
      return <div>
        <HelpText/>
      </div>;
    }

    return <div>
      <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
      <ClusterProfilesWidget elasticProfiles={vnode.state.elasticProfiles()}
                             clusterProfiles={vnode.state.clusterProfiles()}
                             pluginInfos={vnode.state.pluginInfos}
                             elasticAgentOperations={vnode.state.elasticAgentOperations}
                             clusterProfileOperations={vnode.state.clusterProfileOperations}
                             onShowUsages={vnode.state.onShowUsages.bind(vnode.state)}
                             isUserAnAdmin={ElasticProfilesPage.isUserAnAdmin()}/>
    </div>;
  }

  pageName(): string {
    return "Elastic Agents Configuration";
  }

  oninit(vnode: m.Vnode<null, State>) {
    vnode.state.elasticAgentOperations = {
      onClone: (elasticProfile: ElasticAgentProfile, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        new CloneElasticProfileModal(elasticProfile.id()!,
                                     elasticProfile.pluginId()!,
                                     vnode.state.pluginInfos(),
                                     vnode.state.clusterProfiles(),
                                     vnode.state.onSuccessfulSave).render();
      },

      onEdit: (elasticProfile: ElasticAgentProfile, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        vnode.state.elasticProfileBeingEdited(elasticProfile);
        const clusterId = elasticProfile.clusterProfileId();
        if (clusterId) {
          vnode.state.clusterProfileBeingEdited(vnode.state.clusterProfiles().findCluster(clusterId));
        } else {
          throw Error("elastic profile exists without cluster");
        }

        vnode.state.isWizardOpen(true);
        openWizardForEditElasticProfile(vnode.state.pluginInfos,
                                        vnode.state.clusterProfileBeingEdited,
                                        vnode.state.elasticProfileBeingEdited,
                                        vnode.state.onSuccessfulSave,
                                        vnode.state.onError,
                                        this.closeListener(vnode)).next();
      },

      onDelete: (id: string, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        const deleteConfirmMsg = (
          <span>
          Are you sure you want to delete the elastic agent profile <strong>{id}</strong>?
        </span>
        );

        const modal: DeleteConfirmModal = new DeleteConfirmModal(
          deleteConfirmMsg,
          () => {
            return ElasticAgentProfilesCRUD.delete(id)
                                    .then((result) => {
                                      result.do(
                                        () => vnode.state.onSuccessfulSave(
                                          <span>The elastic agent profile <em>{id}</em> was deleted successfully!</span>
                                        ),
                                        onOperationError
                                      );
                                    })
                                    .finally(modal.close.bind(modal));
          });
        modal.render();
      },

      onAdd: (elasticAgentProfile: ElasticAgentProfile, e: MouseEvent) => {
        e.stopPropagation();
        this.flashMessage.clear();

        vnode.state.isWizardOpen(true);
        vnode.state.elasticProfileBeingEdited(elasticAgentProfile);
        const clusterId = elasticAgentProfile.clusterProfileId();
        if (clusterId) {
          vnode.state.clusterProfileBeingEdited(vnode.state.clusterProfiles().findCluster(clusterId));
        } else {
          throw Error("elastic profile exists without cluster");
        }

        vnode.state.isWizardOpen(true);
        return openWizardForAddElasticProfile(vnode.state.pluginInfos,
                                              vnode.state.clusterProfileBeingEdited,
                                              vnode.state.elasticProfileBeingEdited,
                                              vnode.state.onSuccessfulSave,
                                              vnode.state.onError,
                                              this.closeListener(vnode)).next();
      }
    };

    vnode.state.clusterProfileOperations = {
      onEdit: (clusterProfile: ClusterProfile, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();
        vnode.state.clusterProfileBeingEdited(clusterProfile);
        const elasticProfile = new ElasticAgentProfile("",
                                                       vnode.state.clusterProfileBeingEdited().pluginId(),
                                                       vnode.state.clusterProfileBeingEdited().id(),
                                                       new Configurations([]));

        vnode.state.isWizardOpen(true);
        openWizardForEditClusterProfile(vnode.state.pluginInfos,
                                        vnode.state.clusterProfileBeingEdited,
                                        Stream(elasticProfile),
                                        vnode.state.onSuccessfulSave,
                                        vnode.state.onError,
                                        this.closeListener(vnode)).render();
      },

      onDelete: (clusterProfileId: string, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        const deleteConfirmMsg = (
          <span>
          Are you sure you want to delete the cluster profile <strong>{clusterProfileId}</strong>?
        </span>
        );

        const modal: DeleteConfirmModal = new DeleteConfirmModal(
          deleteConfirmMsg,
          () => {
            return ClusterProfilesCRUD.delete(clusterProfileId)
                               .then((result) => {
                                 result.do(
                                   () => vnode.state.onSuccessfulSave(
                                     <span>The cluster profile <em>{clusterProfileId}</em> was deleted successfully!</span>
                                   ),
                                   onOperationError
                                 );
                               })
                               .finally(modal.close.bind(modal));
          });
        modal.render();
      },

      onAdd: (e: MouseEvent) => _.noop(), //for compatibility with ClusterProfileOperations, cleanup when old page is removed

      onClone: (clusterProfile: ClusterProfile, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        new CloneClusterProfileModal(clusterProfile.id()!,
                                     vnode.state.pluginInfos(),
                                     vnode.state.onSuccessfulSave).render();
      },
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(JSON.parse(errorResponse.body!).message);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onClose = () => {
      vnode.state.isWizardOpen(false);
      this.fetchData(vnode);
    };

    vnode.state.onShowUsages = (id: string, event: MouseEvent) => {
      event.stopPropagation();
      this.flashMessage.clear();

      ElasticAgentProfilesCRUD.usage(id).then((result) => {
        result.do(
          (successResponse) => {
            new UsageElasticProfileModal(id, successResponse.body).render();
          },
          onOperationError
        );
      });
    };
    super.oninit(vnode);
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    vnode.state.pluginInfos               = Stream(new PluginInfos());
    vnode.state.clusterProfiles           = Stream(new ClusterProfiles([]));
    vnode.state.elasticProfiles           = Stream(new ElasticAgentProfiles([]));
    vnode.state.clusterProfileBeingEdited = Stream();
    vnode.state.elasticProfileBeingEdited = Stream();
    vnode.state.isWizardOpen              = Stream();

    return Promise.all(
      [
        PluginInfoCRUD.all({type: ExtensionTypeString.ELASTIC_AGENTS}),
        ClusterProfilesCRUD.all(),
        ElasticAgentProfilesCRUD.all(),
      ]
    ).then((results) => {
      results[0].do(
        (successResponse) => {
          this.pageState = PageState.OK;
          vnode.state.pluginInfos(successResponse.body);
        },
        (errorResponse) => {
          this.setErrorState();
          this.flashMessage.setMessage(MessageType.alert, errorResponse.message);
        }
      );
      results[1].do(
        (successResponse) => {
          vnode.state.clusterProfiles(successResponse.body);
          this.pageState = PageState.OK;
        },
        () => this.setErrorState()
      );
      results[2].do(
        (successResponse) => {
          successResponse.body.inferPluginIdFromReferencedCluster(vnode.state.clusterProfiles());
          vnode.state.elasticProfiles(successResponse.body);
          this.pageState = PageState.OK;
        },
        () => this.setErrorState()
      );
    });
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    return <HeaderPanel title={this.pageName()} buttons={this.buttons(vnode)}/>;
  }

  private buttons(vnode: m.Vnode<null, State>) {
    const hasPluginInstalled = vnode.state.pluginInfos().length !== 0;
    const isWizardClose      = vnode.state.isWizardOpen !== undefined && !vnode.state.isWizardOpen();
    if (hasPluginInstalled && isWizardClose) {
      return <Primary onclick={this.addNewClusterProfile.bind(this, vnode)}>Add</Primary>;
    }
  }

  private addNewClusterProfile(vnode: m.Vnode<null, State>) {
    vnode.state.isWizardOpen(true);
    vnode.state.clusterProfileBeingEdited(new ClusterProfile("",
                                                             vnode.state.pluginInfos()[0].id,
                                                             new Configurations([])));
    vnode.state.elasticProfileBeingEdited(new ElasticAgentProfile("",
                                                                  vnode.state.clusterProfileBeingEdited().pluginId(),
                                                                  vnode.state.clusterProfileBeingEdited().id(),
                                                                  new Configurations([])));

    vnode.state.isWizardOpen(true);
    return openWizardForAdd(vnode.state.pluginInfos,
                            vnode.state.clusterProfileBeingEdited,
                            vnode.state.elasticProfileBeingEdited,
                            vnode.state.onSuccessfulSave,
                            vnode.state.onError,
                            this.closeListener(vnode));
  }

  private closeListener(vnode: m.Vnode<null, State>): CloseListener {
    return {
      onClose(): void {
        vnode.state.onClose();
      }
    };
  }
}
