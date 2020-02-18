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

// This is included in one of the layouts (app/views/layouts/pipelines/details.html.erb)
// Invoked when when a template that is associated with a pipeline see `_pipeline_tree.html.erb`

import Stream from "mithril/stream";
import {Template} from "models/admin_templates/templates";
import {TemplatesCRUD} from "models/admin_templates/templates_crud";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {ModalManager} from "views/components/modal/modal_manager";
import {ShowTemplateModal} from "views/pages/admin_templates/modals";

function doRenderModal(pluginInfos: PluginInfos, template: Template) {
  const templateConfig    = Stream(template);
  const showTemplateModal = new ShowTemplateModal(template.name, templateConfig, pluginInfos);
  showTemplateModal.render();
}

document.addEventListener("DOMContentLoaded", () => {
  ModalManager.onPageLoad();
});

// @ts-ignore
window.RenderTemplateInModal = (templateName: string) => {
  PluginInfoCRUD.all({type: ExtensionTypeString.TASK})
                .then((pluginInfoApiResult) => {
                  pluginInfoApiResult
                    .do((pluginInfoSuccessRespone) => {
                      TemplatesCRUD.get(templateName)
                                   .then((templatesApiResult) => {
                                     templatesApiResult.do(
                                       (templatesSuccessResponse) => {
                                         doRenderModal(pluginInfoSuccessRespone.body, templatesSuccessResponse.body);
                                       }, () => {
                                         // ignore
                                       });
                                   });
                    }, (errorResponse) => {
                      // ignore
                    });
                });

};
