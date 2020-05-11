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

import {pipelineEditPath} from "gen/ts-routes";
import {ApiRequestBuilder, ApiResult, ApiVersion, ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Template, TemplateSummary} from "models/admin_templates/templates";
import {TemplatesCRUD} from "models/admin_templates/templates_crud";
import {headerMeta} from "models/current_user_permissions";
import {PipelineStructureWithAdditionalInfo} from "models/internal_pipeline_structure/pipeline_structure";
import {PipelineStructureCRUD} from "models/internal_pipeline_structure/pipeline_structure_crud";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {AnchorVM, ScrollManager} from "views/components/anchor/anchor";
import * as Buttons from "views/components/buttons";
import {ButtonIcon} from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {HeaderPanel} from "views/components/header_panel";
import {ModalState} from "views/components/modal";
import {DeleteConfirmModal} from "views/components/modal/delete_confirm_modal";
import {AdminTemplatesWidget, Attrs, TemplatesScrollOptions} from "views/pages/admin_templates/admin_templates_widget";
import {CreateTemplateModal, ShowTemplateModal} from "views/pages/admin_templates/modals";
import {Page, PageState} from "views/pages/page";
import {EditTemplatePermissionsModal} from "./admin_templates/edit_template_permissions_modal";

const sm: ScrollManager = new AnchorVM();

interface State extends Attrs {
  pluginInfos: PluginInfos;
  usersAutoCompleteHelper: Stream<string[]>;
  rolesAutoCompleteHelper: Stream<string[]>;
}

export class AdminTemplatesPage extends Page<null, State> {
  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    this.parseTemplatesLink(sm);
    const scrollOptions: TemplatesScrollOptions = {
      sm,
      shouldOpenReadOnlyView: (m.route.param().operation || "").toLowerCase() === "view"
    };

    return (
      <div>
        <FlashMessage type={this.flashMessage.type} message={this.flashMessage.message}/>
        <AdminTemplatesWidget {...vnode.state} scrollOptions={scrollOptions}/>
      </div>
    );
  }

  pageName(): string {
    return "Templates";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    const onOperationError              = (errorResponse: ErrorResponse) => {
      vnode.state.onError(JSON.parse(errorResponse.body!).message);
    };
    vnode.state.usersAutoCompleteHelper = Stream();
    vnode.state.rolesAutoCompleteHelper = Stream();

    this.pageState = PageState.LOADING;

    vnode.state.onCreate = () => {
      const modal = new CreateTemplateModal(vnode.state.pipelineStructure, (newTemplateName, basedOnPipeline) => {
        if (_.isEmpty(basedOnPipeline)) {
          TemplatesCRUD.createEmptyTemplate(newTemplateName)
                       .then((apiResult) => {
                         apiResult.do(
                           () => {
                             vnode.state.onSuccessfulSave(<span>A new template <em>{newTemplateName}</em> was created successfully!</span>);
                           },
                           onOperationError
                         );
                       })
                       .finally(() => {
                         modal.close();
                       });
        } else {
          ApiRequestBuilder.PUT(SparkRoutes.adminExtractTemplateFromPipelineConfigPath(basedOnPipeline!),
                                ApiVersion.latest,
                                {
                                  payload: {
                                    template_name: newTemplateName
                                  }
                                }).then((apiResult) => {
            apiResult.do(
              () => {
                const msg = (
                  <span>
                  A new template <em>{newTemplateName}</em> was created successfully. The pipeline <em>{basedOnPipeline}</em> uses this template!
                </span>
                );
                vnode.state.onSuccessfulSave(msg);
              },
              onOperationError);
          });

        }
      });
      modal.render();
    };

    vnode.state.onError = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.alert, msg);
    };

    vnode.state.onSuccessfulSave = (msg: m.Children) => {
      this.flashMessage.setMessage(MessageType.success, msg);
      this.fetchData(vnode);
    };

    vnode.state.onEdit = (template, e) => {
      window.location.href = pipelineEditPath("templates", template.name, "general");
    };

    vnode.state.onDelete = (template, e) => {
      const message                   =
              <span>Are you sure you want to delete the template <em>{template.name}</em>?</span>;
      const modal: DeleteConfirmModal = new DeleteConfirmModal(message, () => {
        return ApiRequestBuilder.DELETE(SparkRoutes.templatesPath(template.name), ApiVersion.latest)
                                .then((apiResponse) => {
                                        apiResponse.do(
                                          (successResponse) => {
                                            vnode.state.onSuccessfulSave(
                                              <span>The template <em>{template.name}</em> was deleted successfully!</span>
                                            );
                                          },
                                          (errorResponse) => {
                                            onOperationError(errorResponse);
                                          }
                                        );
                                      }
                                )
                                .finally(modal.close.bind(modal));
      });
      modal.render();
    };

    vnode.state.doEditPipeline = (pipelineName) => {
      window.location.href = pipelineEditPath("pipelines", pipelineName, "general");
    };

    vnode.state.editPermissions = (template) => {
      if (this.getMeta().showRailsTemplateAuthorization) {
        window.location.href = SparkRoutes.editTemplatePermissions(template.name);
      } else {
        this.pageState = PageState.LOADING;
        TemplatesCRUD.getAuthorization(template.name).then((result) => {
          this.pageState = PageState.OK;
          result.do(
            (successResponse) => {
              new EditTemplatePermissionsModal(template.name, successResponse.body.object, successResponse.body.etag, vnode.state.usersAutoCompleteHelper(), vnode.state.rolesAutoCompleteHelper(), vnode.state.onSuccessfulSave)
                .render();
            }, this.setErrorState);
        });
      }
    };

    vnode.state.doShowTemplate = (templateName) => {
      const templateConfigFromServer = Stream<Template>({} as Template);
      const modal                    = new ShowTemplateModal(templateName,
                                                             templateConfigFromServer,
                                                             vnode.state.pluginInfos);
      modal.modalState               = ModalState.LOADING;
      modal.render();

      TemplatesCRUD.get(templateName)
                   .then((templateResponse) => {
                     templateResponse.do(
                       (successResponse) => {
                         templateConfigFromServer(successResponse.body);
                       },
                       (errorResponse) => {
                         onOperationError(errorResponse);
                         modal.close();
                       });
                   })
                   .finally(() => {
                     modal.modalState = ModalState.OK;
                   });
    };

    return Promise.all([
                         TemplatesCRUD.all(),
                         PipelineStructureCRUD.allPipelines("view", "administer"),
                         PluginInfoCRUD.all({type: ExtensionTypeString.TASK}),
                       ])
                  .then((args) => {
                    const templates: ApiResult<TemplateSummary.TemplateSummaryTemplate[]>   = args[0];
                    const pipelineStructure: ApiResult<PipelineStructureWithAdditionalInfo> = args[1];
                    const pluginInfos: ApiResult<PluginInfos>                               = args[2];

                    templates.do(
                      (successResponse) => {
                        vnode.state.templates = successResponse.body;
                        this.pageState        = PageState.OK;
                      }, (errorResponse) => {
                        this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                        this.pageState = PageState.FAILED;
                      }
                    );

                    pipelineStructure.do(
                      (successResponse) => {
                        vnode.state.pipelineStructure = successResponse.body.pipelineStructure;
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
                        this.pageState          = PageState.OK;
                        vnode.state.pluginInfos = successResponse.body;
                      },
                      (errorResponse) => {
                        this.flashMessage.setMessage(MessageType.alert, JSON.parse(errorResponse.body!).message);
                        this.pageState = PageState.FAILED;
                      }
                    );
                  });
  }

  protected headerPanel(vnode: m.Vnode<null, State>): any {
    const isEnabled     = headerMeta().isUserAdmin || headerMeta().isGroupAdmin;
    const headerButtons = [
      <Buttons.Primary icon={ButtonIcon.ADD}
                       title={isEnabled ? "Create a new template" : "You are not authorized to create a new template. Only system administrators and pipeline group administrators can create templates."}
                       disabled={!isEnabled}
                       onclick={vnode.state.onCreate.bind(vnode.state)}
                       data-test-id="create-new-template">Create a new template</Buttons.Primary>
    ];

    return <HeaderPanel title={this.pageName()} buttons={headerButtons}/>;
  }

  private parseTemplatesLink(sm: ScrollManager) {
    sm.setTarget(m.route.param().name || "");
  }
}
