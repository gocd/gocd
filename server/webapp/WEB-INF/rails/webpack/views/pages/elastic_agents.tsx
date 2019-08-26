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

import {ApiRequestBuilder, ApiResult, ApiVersion, ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
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
import {PipelineStructure} from "models/shared/pipeline_structure/pipeline_structure";
import {PipelineStructureJSON} from "models/shared/pipeline_structure/serialization";
import {ElasticAgentsExtensionType, ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Primary} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {NoPluginsOfTypeInstalled} from "views/components/no_plugins_installed";
import {ElasticProfilesPage} from "views/pages/elastic_agent_configurations";
import {ClusterProfileOperations} from "views/pages/elastic_agent_configurations/cluster_profile_widget";
import {
  CloneClusterProfileModal,
  EditClusterProfileModal,
  NewClusterProfileModal
} from "views/pages/elastic_agent_configurations/cluster_profiles_modals";
import {ClusterProfilesWidget} from "views/pages/elastic_agent_configurations/cluster_profiles_widget";
import {
  CloneElasticProfileModal,
  EditElasticProfileModal, NewElasticProfileModal, UsageElasticProfileModal
} from "views/pages/elastic_agent_configurations/elastic_agent_profiles_modals";
import {ElasticAgentOperations} from "views/pages/elastic_agent_configurations/elastic_profiles_widget";
import {HelpText} from "views/pages/elastic_agents/help_text";
import {openWizard} from "views/pages/elastic_agents/wizard";
import {Page, PageState} from "views/pages/page";
import {RequiresPluginInfos, SaveOperation} from "views/pages/page_operations";

export interface State extends RequiresPluginInfos, SaveOperation {
  onShowUsages: (profileId: string, event: MouseEvent) => void;
  elasticProfiles: Stream<ElasticAgentProfiles>;
  clusterProfiles: Stream<ClusterProfiles>;
  clusterProfileBeingEdited: Stream<ClusterProfile>;
  elasticProfileBeingEdited: Stream<ElasticAgentProfile>;
  pipelineStructure: Stream<PipelineStructure>;
  elasticAgentOperations: ElasticAgentOperations;
  clusterProfileOperations: ClusterProfileOperations;
}

export class PipelineStructureCRUD {
  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.apiAdminInternalPipelinesPath(), ApiVersion.latest)
                            .then((result: ApiResult<string>) => {
                              return result.map((str) => {
                                const data = JSON.parse(str) as PipelineStructureJSON.PipelineStructure;
                                return PipelineStructure.fromJSON(data);
                              });
                            });

  }
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

        new EditElasticProfileModal(elasticProfile.id()!,
                                    elasticProfile.pluginId()!,
                                    vnode.state.pluginInfos(),
                                    vnode.state.clusterProfiles(),
                                    vnode.state.onSuccessfulSave).render();
      },

      onDelete: (id: string, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        const deleteConfirmMsg = (
          <span>
          Are you sure you want to delete the elastic agent profile <strong>{id}</strong>?
        </span>
        );

        const modal = new DeleteConfirmModal(
          deleteConfirmMsg,
          () => {
            ElasticAgentProfilesCRUD.delete(id)
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

        new NewElasticProfileModal(vnode.state.pluginInfos(),
                                   vnode.state.clusterProfiles(),
                                   elasticAgentProfile,
                                   vnode.state.onSuccessfulSave).render();
      }
    };

    vnode.state.clusterProfileOperations = {
      onEdit: (clusterProfile: ClusterProfile, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        new EditClusterProfileModal(clusterProfile.id()!,
                                    vnode.state.pluginInfos(),
                                    vnode.state.onSuccessfulSave).render();
      },

      onDelete: (clusterProfileId: string, event: MouseEvent) => {
        event.stopPropagation();
        this.flashMessage.clear();

        const deleteConfirmMsg = (
          <span>
          Are you sure you want to delete the cluster profile <strong>{clusterProfileId}</strong>?
        </span>
        );

        const modal = new DeleteConfirmModal(
          deleteConfirmMsg,
          () => {
            ClusterProfilesCRUD.delete(clusterProfileId)
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

      onAdd: (e: MouseEvent) => {
        e.stopPropagation();
        this.flashMessage.clear();

        new NewClusterProfileModal(vnode.state.pluginInfos(), vnode.state.onSuccessfulSave).render();
      },

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
    vnode.state.pipelineStructure         = Stream(new PipelineStructure([], []));

    return Promise.all(
      [
        PluginInfoCRUD.all({type: ExtensionTypeString.ELASTIC_AGENTS}),
        PipelineStructureCRUD.all(),
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
          vnode.state.pipelineStructure(successResponse.body);
          this.pageState = PageState.OK;
        },
        () => this.setErrorState()
      );
      results[2].do(
        (successResponse) => {
          vnode.state.clusterProfiles(successResponse.body);
          this.pageState = PageState.OK;
        },
        () => this.setErrorState()
      );
      results[3].do(
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
    if (hasPluginInstalled) {
      return <Primary onclick={this.addNewClusterProfile.bind(this, vnode)}>Add</Primary>;
    }
  }

  private addNewClusterProfile(vnode: m.Vnode<null, State>) {
    vnode.state.clusterProfileBeingEdited(new ClusterProfile("",
                                                             vnode.state.pluginInfos()[0].id,
                                                             new Configurations([])));
    vnode.state.elasticProfileBeingEdited(new ElasticAgentProfile("",
                                                                  vnode.state.clusterProfileBeingEdited().pluginId(),
                                                                  vnode.state.clusterProfileBeingEdited().id(),
                                                                  new Configurations([])));
    return openWizard(vnode.state.pluginInfos,
               vnode.state.clusterProfileBeingEdited,
               vnode.state.elasticProfileBeingEdited,
               vnode.state.pipelineStructure);
  }
}
