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
import classnames from "classnames";
import {ErrorResponse} from "helpers/api_request_builder";
import {override} from "helpers/css_proxies";
import {queryParamAsString} from "helpers/url";
import m from "mithril";
import Stream from "mithril/stream";
import {MaterialConfigFilesJSON} from "models/materials/material_config_files";
import {IDENTIFIER_FORMAT_HELP_MESSAGE} from "views/pages/pipelines/messages";

// models
import {ConfigRepo} from "models/config_repos/types";
import {Material, MaterialAttributes} from "models/materials/types";
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";

// components
import {MaterialCheck} from "views/components/config_repos/material_check.tsx";
import {IdentifierInputField} from "views/components/forms/common_validating_inputs";
import {CheckboxField} from "views/components/forms/input_fields";
import {ShowMore} from "views/components/show_more_doc/more";
import {PacActions} from "views/pages/pac/actions";
import {BuilderForm} from "views/pages/pac/builder_form";
import {CodeScroller} from "views/pages/pac/code_scroller";
import {DownloadAction} from "views/pages/pac/download_action";
import {PreviewPane} from "views/pages/pac/preview_pane";
import {Page, PageState} from "views/pages/page";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {SectionHeading, UserInputPane} from "views/pages/pipelines/user_input_pane";

// CSS
import css from "views/pages/pac/styles.scss";
import fillableCss from "views/pages/pipelines/fillable_section.scss";

const spanningFillableCss = override(fillableCss, {
  fillable: classnames(fillableCss.fillable, css.withSpanningHeading)
});

export class PipelinesAsCodeCreatePage extends Page {
  private pluginId = Stream(ConfigRepo.YAML_PLUGIN_ID);
  private model = new PipelineConfigVM();
  private content = Stream("");
  private mimeType = Stream("application/x-yaml");

  private configRepo = Stream(new ConfigRepo(undefined, this.pluginId(), new Material(this.model.material.type())));
  private useSameRepoForPaC = Stream(true); // allow material sync to be toggleable
  private allowCreate = Stream(false);

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

  onContentChange(isSupportedScm: boolean, updated: boolean) {
    const vm = this.model;

    if (updated) {
      if (this.useSameRepoForPaC() && isSupportedScm) {
        this.configRepo().material(cloneMaterialForPaC(vm.material));
      }

      this.configRepo().pluginId(this.pluginId());

      vm.preview(this.pluginId()).then((result) => {
        if (304 === result.getStatusCode()) {
          return;
        }

        result.do((res) => {
          this.mimeType(result.header("content-type")!.split(";").shift()!);
          this.content(res.body);
        }, (err) => console.error(err)); // tslint:disable-line no-console
      });
    }
  }

  componentToDisplay(vnode: m.Vnode) {
    const vm = this.model;
    const isSupportedScm = pacSupportedMaterial(vm.material);
    const syncConfigRepoWithMaterial = isSupportedScm ? this.useSameRepoForPaC : (val?: boolean) => false;

    return [
      <CodeScroller>
        <FillableSection css={spanningFillableCss}>
          <div class={css.subheading}>
            <h3>1. Build Pipeline Config File</h3>
            <p class={css.sectionNote}><span class={css.attention}>*</span> denotes a required field</p>
          </div>
          <BuilderForm pluginId={this.pluginId} vm={vm} onContentChange={(updated: boolean) => this.onContentChange(isSupportedScm, updated)} onMaterialChange={() => {
            if (syncConfigRepoWithMaterial()) {
              this.allowCreate(false);
            }
          }}/>
          <PreviewPane content={this.content} mimeType={this.mimeType}/>
        </FillableSection>
      </CodeScroller>,

      <FillableSection css={spanningFillableCss}>
        <div class={css.subheading}><h3>2. Add Your Pipelines as Code Definition to Your SCM Repository</h3></div>
        <section class={css.downloadInstructions}>
          <p>Download this configuration and add it to your repository.</p>

          {this.furtherDownloadInstructions(isSupportedScm)}
        </section>

        <DownloadAction pluginId={this.pluginId} vm={vm}/>
      </FillableSection>,

      <FillableSection css={spanningFillableCss}>
        <div class={css.subheading}><SectionHeading>3. Link Your SCM Repository to use the Defined Configuration</SectionHeading></div>

        <UserInputPane onchange={() => { this.allowCreate(false); }}>
          <div class={css.subsectionHeading}>Repository Details</div>
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

        <div class={css.subsection}>
          <div class={css.subsectionHeading}>Label Repository</div>
          <IdentifierInputField label="Config repository name" helpText={IDENTIFIER_FORMAT_HELP_MESSAGE} placeholder="e.g., Pipelines-as-Code-Repository" property={this.configRepo().id} errorText={this.configRepo().errors().errorsForDisplay("id")} required={true}/>
        </div>

        <div class={classnames(css.verifyDefsInMaterial, css.subsection)}>
          <div class={css.subsectionHeading}>Scan Repository for Configuration</div>
          <MaterialCheck pluginId={this.pluginId()} material={this.configRepo().material()!} align="right" prerequisite={() => this.configRepo().isValid()} label={
            <p class={css.msg}>Verify that GoCD can find the configuration file in your repository by clicking the button below</p>
          } success={(data: MaterialConfigFilesJSON, _: string) => {
            this.allowCreate(!!data.plugins.find((p) => !!p.files.length));
          }} failure={(err: ErrorResponse, status: number) => {
            this.allowCreate(413 === status);
          }}/>
        </div>
      </FillableSection>,

      <FillableSection>
        <PacActions configRepo={this.configRepo} disabled={!this.allowCreate()}/>
      </FillableSection>
    ];
  }

  furtherDownloadInstructions(isUsingScmMaterial: boolean) {
    if (isUsingScmMaterial) {
      return <ShowMore abstract={<span><em>Optionally</em>, GoCD allows you to instead store the configuration in a separate repository from one above if desired.</span>}>
        <p>When choosing this option:</p>

        <ol>
          <li>Add the downloaded file to your preferred repository.</li>
          <li>Uncheck the checkbox in the next section and supply the new repository connection details.</li>
        </ol>
      </ShowMore>;
    }
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
