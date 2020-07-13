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

import {ApiRequestBuilder, ApiResult, ApiVersion, ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import m from "mithril";
import Stream from "mithril/stream";
import {PipelineGroupCRUD} from "models/admin_pipelines/pipeline_groups_crud";
import {headerMeta} from "models/current_user_permissions";
import {PipelineGroups, PipelineStructureWithAdditionalInfo} from "models/internal_pipeline_structure/pipeline_structure";
import {PipelineStructureCRUD} from "models/internal_pipeline_structure/pipeline_structure_crud";
import {PipelineGroupCRUD as PipelineGroupCacheCRUD} from "models/pipeline_configs/pipeline_groups_cache";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {AnchorVM, ScrollManager} from "views/components/anchor/anchor";
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {Attrs, PipelineGroupsWidget, PipelinesScrollOptions} from "views/pages/admin_pipelines/admin_pipelines_widget";
import {
  ClonePipelineConfigModal,
  CreatePipelineGroupModal,
  DeletePipelineGroupModal,
  DownloadPipelineModal,
  ExtractTemplateModal,
  MoveConfirmModal
} from "views/pages/admin_pipelines/modals";
import {Page, PageState} from "views/pages/page";
import buttonStyle from "views/pages/pipelines/actions.scss";
import {EditPipelineGroupModal} from "./admin_pipelines/edit_pipeline_group_modal";

interface State extends Attrs {
  pluginInfos: Stream<PluginInfos>;
  usersAutoCompleteHelper: Stream<string[]>;
  rolesAutoCompleteHelper: Stream<string[]>;
}

const sm: ScrollManager = new AnchorVM();

export class AdminPipelinesPage extends Page<null, State> {
  private operation: string = "";

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    this.parseRepoLink(sm);
    const scrollOptions: PipelinesScrollOptions = {
      sm,
      shouldOpenEditView: this.operation === "edit"
    };
    return (
      <div>
        <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
        <PipelineGroupsWidget {...vnode.state} scrollOptions={scrollOptions}/>
      </div>
    );
  }

  pageName(): string {
    return "Pipelines";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    this.pageState = PageState.LOADING;

    vnode.state.pipelineGroups          = Stream(new PipelineGroups());
    vnode.state.pluginInfos             = Stream(new PluginInfos());
    vnode.state.usersAutoCompleteHelper = Stream();
    vnode.state.rolesAutoCompleteHelper = Stream();

    const onOperationError = (errorResponse: ErrorResponse) => {
      vnode.state.onError(JSON.parse(errorResponse.body!).message);
    };

    vnode.state.doEditPipelineGroup = (groupName: string) => {
      const pipelineGroup             = vnode.state.pipelineGroups().find((pipelineGroup) => pipelineGroup.name() === groupName);
      const containsPipelinesRemotely = pipelineGroup ? pipelineGroup.containsRemotelyDefinedPipelines() : false;

      this.pageState = PageState.LOADING;
      PipelineGroupCRUD.get(groupName).then((result) => {
        this.pageState = PageState.OK;
        result.do((successResponse) => {
          new EditPipelineGroupModal(successResponse.body.object, successResponse.body.etag, vnode.state.usersAutoCompleteHelper(), vnode.state.rolesAutoCompleteHelper(), vnode.state.onSuccessfulSave, containsPipelinesRemotely).render();
        }, () => {
          this.setErrorState();
        });
      });
    };

    vnode.state.createPipelineGroup = () => {
      const modal = new CreatePipelineGroupModal((groupName) => {
        PipelineGroupCacheCRUD
          .create({name: groupName})
          .then((response) => {
            response.do(
              (successResponse) => {
                const msg = <span>The pipeline group <em>{groupName}</em> was created successfully.</span>;
                vnode.state.onSuccessfulSave(msg);
              },
              onOperationError);
          })
        ;
      });
      modal.render();
    };

    vnode.state.createPipelineInGroup = (groupName) => {
      window.location.href = SparkRoutes.newCreatePipelinePath(groupName);
    };

    vnode.state.doMovePipeline = (sourceGroup, pipeline) => {
      new MoveConfirmModal(vnode.state.pipelineGroups(),
                           sourceGroup,
                           pipeline,
                           vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.doDeleteGroup = (group) => {
      new DeletePipelineGroupModal(group.name(), vnode.state.onSuccessfulSave)
        .render();
    };

    vnode.state.doDeletePipeline = (pipeline) => {
      const message = <span>Are you sure you want to delete the pipeline <em>{pipeline.name()}</em>?</span>;

      const modal: DeleteConfirmModal = new DeleteConfirmModal(message, () => {
        return ApiRequestBuilder.DELETE(SparkRoutes.adminPipelineConfigPath(pipeline.name()), ApiVersion.latest)
                                .then((result) => {
                                  result.do(
                                    () => vnode.state.onSuccessfulSave(
                                      <span>The pipeline <em>{pipeline.name()}</em> was deleted successfully!</span>
                                    ),
                                    onOperationError
                                  );

                                })
                                .finally(modal.close.bind(modal));
      });

      modal.render();
    };

    vnode.state.doExtractPipeline = (pipeline) => {
      const extractOperation = (templateName: string) => {
        ApiRequestBuilder.PUT(SparkRoutes.adminExtractTemplateFromPipelineConfigPath(pipeline.name()),
                              ApiVersion.latest,
                              {
                                payload: {
                                  template_name: templateName
                                }
                              }).then((apiResult) => {
          apiResult.do(
            (successResponse) => {
              const msg = (
                <span>
                  A new template <em>{templateName}</em> was created successfully. The pipeline <em>{pipeline.name()}</em> uses this template!
                </span>
              );
              vnode.state.onSuccessfulSave(msg);
            },
            onOperationError);
        });
      };

      const modal = new ExtractTemplateModal(pipeline.name(), extractOperation);
      modal.render();
    };

    vnode.state.doEditPipeline = (pipeline) => {
      window.location.href = SparkRoutes.pipelineEditPath('pipelines', pipeline.name(), 'general');
    };

    vnode.state.doClonePipeline = (shallowPipeline) => {
      const cloneOperation = (newPipelineName: string) => {
        const newPipeline = shallowPipeline.clone();
        newPipeline.name(newPipelineName);
        vnode.state.doEditPipeline(newPipeline);
      };

      new ClonePipelineConfigModal(shallowPipeline, cloneOperation)
        .render();
    };

    vnode.state.doDownloadPipeline = (pipeline) => {
      const requestHandle = Stream<XMLHttpRequest>();
      const modal         = new DownloadPipelineModal(pipeline, vnode.state.pluginInfos(), (pluginId) => {
        m.request(SparkRoutes.exportPipelinePath(pluginId, pipeline.name()), {
          headers: {
            "Accept":           ApiRequestBuilder.versionHeader(ApiVersion.latest),
            "X-Requested-With": "XMLHttpRequest"
          },
          config(xhr: XMLHttpRequest) {
            requestHandle(xhr);
            xhr.responseType = "blob";
          }
        }).then(() => {
          const fileName = requestHandle()
            .getResponseHeader("Content-Disposition")!
            .replace(/^attachment; filename=/, "")
            .replace(/^(")(.+)(\1)/, "$2");

          const blobUrl = URL.createObjectURL(requestHandle().response);

          const tempLink = document.createElement("a");

          tempLink.setAttribute("href", blobUrl);
          tempLink.setAttribute("download", fileName);
          tempLink.style.display = "none";

          document.body.appendChild(tempLink); // Firefox requires this to be added to the DOM before click()
          tempLink.click();
          document.body.removeChild(tempLink);
          URL.revokeObjectURL(blobUrl);
        }).catch((error) => {
          const msg = "There was an unknown error downloading the pipeline configuration. Please refresh the page and try again.";
          vnode.state.onError(msg);
          this.scrollToTop();
        }).finally(() => {
          modal.close();
        });
      });
      modal.render();
    };

    return Promise.all([PipelineStructureCRUD.allPipelines("administer", "view"),
                         PluginInfoCRUD.all({type: ExtensionTypeString.CONFIG_REPO})
                       ]).then((args) => {
      const pipelineGroups: ApiResult<PipelineStructureWithAdditionalInfo> = args[0];
      const pluginInfos: ApiResult<PluginInfos>                            = args[1];

      pipelineGroups.do(
        (successResponse) => {
          vnode.state.pipelineGroups(successResponse.body.pipelineStructure.groups());
          vnode.state.usersAutoCompleteHelper(successResponse.body.additionalInfo.users);
          vnode.state.rolesAutoCompleteHelper(successResponse.body.additionalInfo.roles);
          this.pageState = PageState.OK;
        }, (errorResponse) => {
          this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
          this.pageState = PageState.FAILED;
        }
      );

      pluginInfos.do(
        (successResponse) => {
          vnode.state.pluginInfos(successResponse.body);
          this.pageState = PageState.OK;
        }, (errorResponse) => {
          this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
          this.pageState = PageState.FAILED;
        }
      );
    });
  }

  parseRepoLink(sm: ScrollManager) {
    sm.setTarget(m.route.param().id || "");
    this.operation = (m.route.param().operation || "").toLowerCase();
  }

  helpText(): m.Children {
    return PipelineGroupsWidget.helpTextWhenEmpty();
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const headerButtons = [
      <Buttons.Secondary css={buttonStyle}
                         icon={ButtonIcon.ADD}
                         disabled={!headerMeta().isUserAdmin}
                         title={headerMeta().isUserAdmin ? "Create a new pipeline group" : "Only GoCD system administrators are allowed to create a pipeline group."}
                         onclick={vnode.state.createPipelineGroup.bind(vnode.state)}
                         data-test-id="create-new-pipeline-group">Create new pipeline group</Buttons.Secondary>
    ];
    return <HeaderPanel title={this.pageName()} buttons={headerButtons} help={this.helpText()}/>;
  }
}
