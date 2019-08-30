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

// utils
import {queryParamAsString} from "helpers/url";
import m from "mithril";
import Stream from "mithril/stream";

// models
import {Material, MaterialAttributes} from "models/materials/types";
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";

// components
import {PacActions} from "views/pages/pac/actions";
import {BuilderForm} from "views/pages/pac/builder_form";
import {DownloadAction} from "views/pages/pac/download_action";
import {PreviewPane} from "views/pages/pac/preview_pane";
import {Page, PageState} from "views/pages/page";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import {MaterialEditor} from "views/pages/pipelines/material_editor";

export class PipelinesAsCodeCreatePage extends Page {
  private model = new PipelineConfigVM();
  private content = Stream("");
  private mimeType = Stream("application/x-yaml");

  private material = Stream(new Material(this.model.material.type()));

  oninit(vnode: m.Vnode) {
    this.pageState = PageState.OK;
    const group = queryParamAsString(window.location.search, "group").trim();

    if ("" !== group) {
      this.model.pipeline.group(group);
    }
  }

  pageName() {
    return "Add a New Pipeline as Code";
  }

  componentToDisplay(vnode: m.Vnode) {
    const vm = this.model;

    return [
      <FillableSection>
        <BuilderForm vm={vm} onContentChange={(updated) => {
          if (updated) {
            if (pacSupportedMaterial(vm.material)) {
              this.material(cloneMaterialForPaC(vm.material));
            }

            vm.preview(vm.pluginId()).then((result) => {
              if (304 === result.getStatusCode()) {
                return;
              }

              result.do((res) => {
                this.content(res.body);
              }, (err) => console.error(err)); // tslint:disable-line no-console
            });
          }
        }}/>

        <PreviewPane content={this.content} mimeType={this.mimeType}/>
      </FillableSection>,

      <FillableSection>
        <div>Download this as a file and put it in your repo (I need some proper copy here).</div>

        <DownloadAction vm={vm}/>
      </FillableSection>,

      <FillableSection>
        <div>
          <span>Use the 'Check Material' button to verify that the configuration file is corretly placed</span>

          <MaterialEditor material={this.material()} materialCheck={true} scmOnly={true} showLocalWorkingCopyOptions={false}/>
        </div>
      </FillableSection>,

      <FillableSection>
        <PacActions material={this.material} pluginId={vm.pluginId} />
      </FillableSection>
    ];
  }

  fetchData() { return new Promise(() => null); }
}

// tests if we can use a given material for config repos
function pacSupportedMaterial(material: Material) {
  const type = material.type();
  return !!type && !["dependency", "package", "plugin"].includes(type);
}

// duplicates a material for use with config repos; this removes the material
// name and destination from the copy
function cloneMaterialForPaC(orig: Material): Material {
  const json = orig.toApiPayload();
  delete json.attributes.name;
  delete json.attributes.destination;

  return new Material(json.type, MaterialAttributes.deserialize(json));
}
