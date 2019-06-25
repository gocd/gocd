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
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Page, PageState} from "views/pages/page";

// models
import {GitMaterialAttributes, Material} from "models/materials/types";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";

// components
import {PipelineActions} from "views/pages/pipelines/actions";
import {AdvancedSettings} from "views/pages/pipelines/advanced_settings";
import {ConceptDiagram} from "views/pages/pipelines/concept_diagram";
import {EnvironmentVariablesEditor} from "views/pages/pipelines/environment_variables_editor";
import {FillableSection} from "views/pages/pipelines/fillable_section";
import {JobEditor} from "views/pages/pipelines/job_editor";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {PipelineInfoEditor} from "views/pages/pipelines/pipeline_info_editor";
import {StageEditor} from "views/pages/pipelines/stage_editor";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";
import {UserInputPane} from "views/pages/pipelines/user_input_pane";

const materialImg = require("../../../app/assets/images/concept_diagrams/concept_material.svg");
const pipelineImg = require("../../../app/assets/images/concept_diagrams/concept_pipeline.svg");
const stageImg    = require("../../../app/assets/images/concept_diagrams/concept_stage.svg");
const jobImg      = require("../../../app/assets/images/concept_diagrams/concept_job.svg");

export class PipelineCreatePage extends Page {
  private material: Material = new Material("git", new GitMaterialAttributes());
  private job: Job = new Job("", [], []);
  private stage: Stage = new Stage("", [this.job]);
  private model: PipelineConfig = new PipelineConfig("", [this.material], []);
  private isUsingTemplate: Stream<boolean> = stream(false);

  pageName(): string {
    return "Add a New Pipeline";
  }

  oninit(vnode: m.Vnode) {
    this.pageState = PageState.OK;
    const group = m.parseQueryString(window.location.search).group;
    if ("" !== String(group || "").trim()) {
      this.model.group(group);
    }
  }

  componentToDisplay(vnode: m.Vnode): m.Children {
    const components = [
      <FillableSection>
        <UserInputPane heading="Part 1: Material">
          <MaterialEditor material={this.material} group={this.model.group()}/>
        </UserInputPane>
        <ConceptDiagram image={materialImg}>
          A <strong>material</strong> triggers your pipeline to run. Typically this is a <strong>source repository</strong> or an <strong>upstream pipeline</strong>.
        </ConceptDiagram>
      </FillableSection>,

      <FillableSection>
        <UserInputPane heading="Part 2: Pipeline Name">
          <PipelineInfoEditor pipelineConfig={this.model} isUsingTemplate={this.isUsingTemplate}/>
        </UserInputPane>
        <ConceptDiagram image={pipelineImg}>
          In GoCD, a <strong>pipeline</strong> is a representation of a <strong>workflow</strong>. Pipelines consist of one or more <strong>stages</strong>.
        </ConceptDiagram>
      </FillableSection>];

    if (!this.isUsingTemplate() ) {
      if (!this.model.stages().has(this.stage)) {
        this.model.stages(new NameableSet([this.stage]));
      }
      components.push(
        <FillableSection>
          <UserInputPane heading="Part 3: Stage Details">
            <StageEditor stage={this.stage} />
          </UserInputPane>
          <ConceptDiagram image={stageImg}>
            A <strong>stage</strong> is a group of jobs, and a <strong>job</strong> is a piece of work to execute.
          </ConceptDiagram>
        </FillableSection>,
      <FillableSection>
        <UserInputPane heading="Part 4: Job and Tasks">
          <JobEditor job={this.job}/>
          <TaskTerminalField label="Type your tasks below at the prompt" property={this.job.tasks} errorText={this.job.errors().errorsForDisplay("tasks")} required={true}/>
          <AdvancedSettings forceOpen={_.some(this.job.environmentVariables(), (env) => env.errors().hasErrors())}>
            <EnvironmentVariablesEditor variables={this.job.environmentVariables}  />
          </AdvancedSettings>
        </UserInputPane>
        <ConceptDiagram image={jobImg}>
          A <strong>job</strong> is like a script, where each sequential step is called a <strong>task</strong>. Typically, a task is a single command.
        </ConceptDiagram>
      </FillableSection>
      );
    }

    components.push(<PipelineActions pipelineConfig={this.model}/>);

    return components;
  }

  fetchData(vnode: m.Vnode): Promise<any> {
    return new Promise(() => null);
  }
}
