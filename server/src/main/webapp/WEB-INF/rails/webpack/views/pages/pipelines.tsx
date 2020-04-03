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

// utils
import {queryParamAsString} from "helpers/url";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Scms} from "models/materials/pluggable_scm";
import {PluggableScmCRUD} from "models/materials/pluggable_scm_crud";
import {PackagesCRUD} from "models/package_repositories/packages_crud";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {PackageRepositoriesCRUD} from "models/package_repositories/package_repositories_crud";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
// components
import {ConceptDiagram} from "views/components/concept_diagram";
import {EnvironmentVariablesWidget} from "views/components/environment_variables";
import {Page, PageState} from "views/pages/page";
import {PipelineActions} from "views/pages/pipelines/actions";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import {JobEditor} from "views/pages/pipelines/job_editor";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
// models
import {PipelineConfigVM} from "views/pages/pipelines/pipeline_config_view_model";
import {PipelineInfoEditor} from "views/pages/pipelines/pipeline_info_editor";
import {StageEditor} from "views/pages/pipelines/stage_editor";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";
import {UserInputPane} from "views/pages/pipelines/user_input_pane";
import {PackageRepositoriesPage} from "./package_repositories";

const materialImg = require("../../../app/assets/images/concept_diagrams/concept_material.svg");
const pipelineImg = require("../../../app/assets/images/concept_diagrams/concept_pipeline.svg");
const stageImg    = require("../../../app/assets/images/concept_diagrams/concept_stage.svg");
const jobImg      = require("../../../app/assets/images/concept_diagrams/concept_job.svg");

interface State {
  pluginInfos: Stream<PluginInfos>;
  packageRepositories: Stream<PackageRepositories>;
  packages: Stream<Packages>;
  scmMaterials: Stream<Scms>;
}

export class PipelineCreatePage extends Page<{}, State> {
  private model = new PipelineConfigVM();

  pageName(): string {
    return "Add a New Pipeline";
  }

  oninit(vnode: m.Vnode<{}, State>) {
    super.oninit(vnode);
    const group = queryParamAsString(window.location.search, "group").trim();

    if ("" !== group) {
      this.model.pipeline.group(group);
    }

    vnode.state.pluginInfos         = Stream(new PluginInfos());
    vnode.state.packageRepositories = Stream();
    vnode.state.packages            = Stream();
    vnode.state.scmMaterials        = Stream();
  }

  componentToDisplay(vnode: m.Vnode<{}, State>): m.Children {
    const {pipeline, material, stage, job, isUsingTemplate} = this.model;
    const mergedPkgRepos                                    = PackageRepositoriesPage.getMergedList(vnode.state.packageRepositories, vnode.state.packages);
    return [
      <FillableSection>
        <UserInputPane heading="Part 1: Material">
          <MaterialEditor material={material} showExtraMaterials={true} pluggableScms={vnode.state.scmMaterials()}
                          packageRepositories={mergedPkgRepos} pluginInfos={vnode.state.pluginInfos()}/>
        </UserInputPane>
        <ConceptDiagram image={materialImg}>
          A <strong>material</strong> triggers your pipeline to run. Typically this is a <strong>source repository</strong> or an <strong>upStream
          pipeline</strong>.
        </ConceptDiagram>
      </FillableSection>,

      <FillableSection>
        <UserInputPane heading="Part 2: Pipeline Name">
          <PipelineInfoEditor pipelineConfig={pipeline} isUsingTemplate={isUsingTemplate}/>
        </UserInputPane>
        <ConceptDiagram image={pipelineImg}>
          In GoCD, a <strong>pipeline</strong> is a representation of a <strong>workflow</strong>. Pipelines consist of one or
          more <strong>stages</strong>.
        </ConceptDiagram>
      </FillableSection>,

      this.model.whenTemplateAbsent(() => [
        <FillableSection>
          <UserInputPane heading="Part 3: Stage Details">
            <StageEditor stage={stage}/>
          </UserInputPane>
          <ConceptDiagram image={stageImg}>
            A <strong>stage</strong> is a group of jobs, and a <strong>job</strong> is a piece of work to execute.
          </ConceptDiagram>
        </FillableSection>,

        <FillableSection>
          <UserInputPane heading="Part 4: Job and Tasks">
            <JobEditor job={job}/>
            <TaskTerminalField label="Type your tasks below at the prompt" property={job.tasks} errorText={job.errors().errorsForDisplay("tasks")}
                               required={true}/>
            <AdvancedSettings forceOpen={_.some(job.environmentVariables(), (env) => env.errors().hasErrors())}>
              <EnvironmentVariablesWidget environmentVariables={job.environmentVariables()}/>
            </AdvancedSettings>
          </UserInputPane>
          <ConceptDiagram image={jobImg}>
            A <strong>job</strong> is like a script, where each sequential step is called a <strong>task</strong>. Typically, a task is a single
            command.
          </ConceptDiagram>
        </FillableSection>
      ]),

      <PipelineActions pipelineConfig={pipeline}/>
    ];
  }

  fetchData(vnode: m.Vnode<{}, State>): Promise<any> {
    return Promise.all([PluginInfoCRUD.all({type: ExtensionTypeString.PACKAGE_REPO}), PluginInfoCRUD.all({type: ExtensionTypeString.SCM}),
                         PackageRepositoriesCRUD.all(), PackagesCRUD.all(), PluggableScmCRUD.all()])
                  .then((result) => {
                    [result[0], result[1]]
                      .forEach((apiResult) => apiResult.do((successResponse) => {
                        vnode.state.pluginInfos().push(...successResponse.body);
                        this.pageState = PageState.OK;
                      }, this.setErrorState));

                    result[2].do((successResponse) => {
                      vnode.state.packageRepositories(successResponse.body);
                      this.pageState = PageState.OK;
                    }, this.setErrorState);

                    result[3].do((successResponse) => {
                      vnode.state.packages(successResponse.body);
                      this.pageState = PageState.OK;
                    }, this.setErrorState);

                    result[4].do((successResponse) => {
                      vnode.state.scmMaterials(successResponse.body);
                      this.pageState = PageState.OK;
                    }, this.setErrorState);
                  });
  }
}
