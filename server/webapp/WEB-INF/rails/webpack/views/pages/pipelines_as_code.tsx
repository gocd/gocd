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
import {override} from "helpers/css_proxies";
import {queryParamAsString} from "helpers/url";
import m from "mithril";
import Stream from "mithril/stream";

// models
import {ConfigRepo} from "models/config_repos/types";
import {Material, MaterialAttributes} from "models/materials/types";
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";

// components
import {MaterialCheck} from "views/components/config_repos/material_check.tsx";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {CheckboxField} from "views/components/forms/input_fields";
import {PacActions} from "views/pages/pac/actions";
import {BuilderForm} from "views/pages/pac/builder_form";
import {DownloadAction} from "views/pages/pac/download_action";
import {PreviewPane} from "views/pages/pac/preview_pane";
import css from "views/pages/pac/styles.scss";
import {Page, PageState} from "views/pages/page";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import fillableCss from "views/pages/pipelines/fillable_section.scss";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {UserInputPane} from "views/pages/pipelines/user_input_pane";

const altFillStyles = override(fillableCss, {
  fillable: [fillableCss.fillable, css.withSpanningHeading].join(" ")
});

export class PipelinesAsCodeCreatePage extends Page {
  private pluginId = Stream(ConfigRepo.YAML_PLUGIN_ID);
  private model = new PipelineConfigVM();
  private content = Stream("");
  private mimeType = Stream("application/x-yaml");

  private configRepo = Stream(new ConfigRepo(undefined, this.pluginId(), new Material(this.model.material.type())));
  private useSameRepoForPaC = Stream(true); // allow material sync to be toggleable

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
    const isSupportedScm = pacSupportedMaterial(vm.material);
    const syncConfigRepoWithMaterial = isSupportedScm ? this.useSameRepoForPaC : (val?: boolean) => false;

    return [
      <FillableSection>
        <BuilderForm vm={vm} pluginId={this.pluginId} onContentChange={(updated) => {
          if (updated) {
            if (this.useSameRepoForPaC() && isSupportedScm) {
              this.configRepo().material(cloneMaterialForPaC(vm.material));
            }

            vm.preview(this.pluginId()).then((result) => {
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

      <FillableSection css={altFillStyles}>
        <h3 class={css.subheading}>Add Your Pipelines as Code Definition to Your SCM Repository</h3>
        <div>Download this as a file and put it in your repo (I need some proper copy here).</div>

        <DownloadAction pluginId={this.pluginId} vm={vm}/>
      </FillableSection>,

      <FillableSection>
        <UserInputPane heading="Register Your Pipelines as Code Repo with GoCD">
          <CheckboxField
            property={syncConfigRepoWithMaterial}
            label={<span>Use the same SCM repository from the form above to store my <strong>Pipelines as Code</strong> definitions <em class={css.hint}>(suitable for most setups)</em></span>}
            readonly={!isSupportedScm}
            onchange={() => {
              if (syncConfigRepoWithMaterial()) {
                this.configRepo().material(cloneMaterialForPaC(vm.material));
              }
            }}
          />

          <MaterialEditor material={this.configRepo().material()!} hideTestConnection={syncConfigRepoWithMaterial()} scmOnly={true} showLocalWorkingCopyOptions={false} disabled={syncConfigRepoWithMaterial()}/>
        </UserInputPane>
        <div class={css.verifyDefsInMaterial}>
          <IdentifierInputField label="Name" property={this.configRepo().id} errorText={this.configRepo().errors().errorsForDisplay("id")} />
          <MaterialCheck pluginId={this.pluginId()} material={this.configRepo().material()!} align="right" prerequisite={() => this.configRepo().material()!.isValid()} label={
            <p class={css.msg}>Click the button to verify that the configuration file is placed correctly in the repository</p>
          }/>
        </div>
      </FillableSection>,

      <FillableSection>
        <PacActions configRepo={this.configRepo} />
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
  if (!pacSupportedMaterial(orig)) {
    return new Material("git", MaterialAttributes.deserialize({ type: "git", attributes: {} as any }));
  }

  const json = orig.toApiPayload();
  delete json.attributes.name;
  delete json.attributes.destination;

  return new Material(json.type, MaterialAttributes.deserialize(json));
}
