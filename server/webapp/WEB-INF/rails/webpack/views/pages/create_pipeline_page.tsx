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

import m from "mithril";
import {Material} from "models/new_pipeline_configs/materials";
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import {Page} from "views/pages/page";
import {PipelineConfigCreateWidget} from "views/pages/pipeline_configs/pipeline_config_create_widget";
import {PipelineSettingsModal} from "views/pages/pipeline_configs/settings/pipeline_settings_modal";

export interface MaterialOperations {
  onAdd: (material: Material) => void;
  onDelete: (material: Material) => void;
  onUpdate: (oldMaterial: Material, newMaterial: Material) => void;
}

interface State {
  materialOperations: MaterialOperations;
  pipelineConfig: PipelineConfig;
  showPipelineSettings: (e: Event) => void;
}

export const SUPPORTED_MATERIALS = [
  {id: "git", text: "Git"},
  {id: "hg", text: "Mercurial"},
  {id: "svn", text: "Subversion"},
  {id: "p4", text: "Perforce"},
  {id: "tfs", text: "Team Foundation Server"},
  {id: "dependency", text: "Another Pipeline"},
];

export class CreatePipelinePage extends Page<null, State> {
  oninit(vnode: m.Vnode<null, State>) {
    super.oninit(vnode);
    vnode.state.pipelineConfig = new PipelineConfig("", [], []);

    vnode.state.materialOperations = {
      onAdd(material: Material) {
        vnode.state.pipelineConfig.materials().push(material);
      },
      onDelete(material: Material) {
        vnode.state.pipelineConfig.materials().remove(material);
      },
      onUpdate(oldMaterial: Material, newMaterial: Material) {
        vnode.state.pipelineConfig.materials().remove(oldMaterial);
        vnode.state.pipelineConfig.materials().push(newMaterial);
      }
    };

    vnode.state.showPipelineSettings = (e: Event) => {
      e.stopPropagation();
      new PipelineSettingsModal(vnode.state.pipelineConfig).render();
    };
  }

  componentToDisplay(vnode: m.Vnode<null, State>): m.Children {
    return <PipelineConfigCreateWidget {...vnode.state} onPipelineSettingsEdit={vnode.state.showPipelineSettings}/>;
  }

  pageName(): string {
    return "New Pipeline";
  }

  fetchData(vnode: m.Vnode<null, State>): Promise<any> {
    // to be implemented
    return Promise.resolve();
  }
}
