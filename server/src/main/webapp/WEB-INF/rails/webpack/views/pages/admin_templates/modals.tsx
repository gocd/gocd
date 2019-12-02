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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Artifact, Job, Stage, Tab, Task, Template} from "models/admin_templates/templates";
import {EnvironmentVariableJSON} from "models/environment_variables/types";
import {PipelineStructure} from "models/internal_pipeline_structure/pipeline_structure";
import {ModelWithNameIdentifierValidator} from "models/shared/name_validation";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessage, MessageType} from "views/components/flash_message";
import {CheckboxField, SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Tree} from "views/components/hierarchy/tree";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import {Modal, Size} from "views/components/modal";
import {Tabs} from "views/components/tab";
import {Table} from "views/components/table";
import styles from "views/pages/admin_templates/modals.scss";
import {TaskWidget} from "views/pages/admin_templates/task_widget";

const inflection = require("lodash-inflection");

export class CreateTemplateModal extends Modal {
  private readonly callback: (newTemplateName: string, basedOnPipeline?: string) => void;
  private readonly template: ModelWithNameIdentifierValidator;
  private readonly basedOnPipelineCheckbox: Stream<boolean>;
  private readonly selectedPipeline: Stream<string>;
  private readonly pipelines: string[];

  constructor(pipelineStructure: PipelineStructure,
              callback: (newTemplateName: string, basedOnPipeline?: string) => void) {
    super();
    this.callback                = callback;
    this.template                = new ModelWithNameIdentifierValidator();
    this.basedOnPipelineCheckbox = Stream<boolean>(false);
    this.selectedPipeline        = Stream<string>();
    this.pipelines               = pipelineStructure.getAllConfigPipelinesNotUsingTemplates().sort((a, b) => {
      return a.toLowerCase().localeCompare(b.toLowerCase());
    });
  }

  body() {
    return (
      <div>
        <TextField property={this.template.name}
                   errorText={this.template.errors().errorsForDisplay("name")}
                   onchange={() => this.template.validate("name")}
                   required={true}
                   label={"Template name"}/>

        <CheckboxField property={this.basedOnPipelineCheckbox}
                       label={"Extract from pipeline"}
                       helpText={"If a pipeline is not selected, a template with a default stage and default job will be created. If a pipeline is selected, the template will use the stages from the pipeline and the pipeline itself will be modified to use this template."}/>
        {this.maybeShowPipelines()}
      </div>
    );
  }

  buttons() {
    const disabled = _.isEmpty(this.template.name()) ||
      this.template.errors().hasErrors() ||
      (this.basedOnPipelineCheckbox() && _.isEmpty(this.selectedPipeline()));

    return [<Buttons.Primary data-test-id="button-create"
                             disabled={disabled}
                             onclick={this.create.bind(this)}>Create</Buttons.Primary>];
  }

  title(): string {
    return "Create a new template";
  }

  private create() {
    this.callback(this.template.name(), this.basedOnPipelineCheckbox() ? this.selectedPipeline() : undefined);
    super.close();
  }

  private maybeShowPipelines() {
    if (this.basedOnPipelineCheckbox()) {
      return (
        <SelectField property={this.selectedPipeline}
                     label={"Pipeline"}
                     helpText={"This pipeline will be modified to use the newly created template."}>
          <SelectFieldOptions items={this.pipelines} selected={this.selectedPipeline()}/>
        </SelectField>
      );
    }
  }
}

export class ShowTemplateModal extends Modal {
  private readonly template: string;
  private readonly templateConfig: Stream<Template>;
  private readonly pluginInfos: PluginInfos;
  private selectedStage?: Stage;
  private selectedJob?: Job;

  constructor(template: string, templateConfig: Stream<Template>, pluginInfos: PluginInfos) {
    super(Size.large);
    this.template       = template;
    this.templateConfig = templateConfig;
    this.pluginInfos    = pluginInfos;
    this.templateConfig();
  }

  body() {
    if (this.isLoading()) {
      return undefined;
    }

    return (
      <div class={styles.parent}>
        <div data-test-id="stage-job-tree" class={styles.stageJobTree}>
          {this.templateConfig().stages.map((eachStage) => {
            const stageLink = (
              <Link href="#" onclick={() => {
                this.selectStage(eachStage);
                return false;
              }}>{eachStage.name}</Link>
            );
            return (
              <Tree datum={stageLink}>
                {eachStage.jobs.map((eachJob) => {
                  const jobLink = (
                    <Link href="#" onclick={() => {
                      this.selectJob(eachStage, eachJob);
                      return false;
                    }}>{eachJob.name}</Link>
                  );
                  return (
                    <Tree datum={jobLink}/>
                  );
                })}
              </Tree>);
          })}
        </div>

        {this.showSelection()}
      </div>
    );
  }

  title(): string {
    return `Showing template ${this.template}`;
  }

  private selectStage(eachStage: Stage) {
    this.selectedStage = eachStage;
    this.selectedJob   = undefined;
  }

  private selectJob(eachStage: Stage, eachJob: Job) {
    this.selectedStage = eachStage;
    this.selectedJob   = eachJob;
  }

  private showSelection() {
    if (!this.selectedJob && !this.selectedStage) {
      this.selectStage(this.templateConfig().stages[0]);
    }
    if (this.selectedJob) {
      return this.showJob(this.selectedStage!, this.selectedJob!);
    }

    return this.showStage(this.selectedStage!);
  }

  private showStage(stage: Stage) {
    const stageProperties = new Map([
                                      ["Stage Type", stage.approval.type === "success" ? "On success" : "Manual"],
                                      ["Fetch Materials", this.yesOrNo(stage.fetch_materials)],
                                      ["Never Cleanup Artifacts", this.yesOrNo(stage.never_cleanup_artifacts)],
                                      ["Clean Working Directory", this.yesOrNo(stage.clean_working_directory)],
                                    ]);
    return (
      <div data-test-id={`selected-stage-${stage.name}`} class={styles.stageOrJob}>
        Showing stage <em>{stage.name}</em>
        <hr/>
        <KeyValuePair data={stageProperties}/>
        <Tabs
          tabs={["Environment Variables", "Permissions"]}
          contents={
            [this.environmentVariables(stage.environment_variables), this.stagePermissions(stage)]}/>
      </div>
    );
  }

  private showJob(stage: Stage, job: Job) {
    const jobProperties = new Map<string, any>([
                                                 ["Resources", _.isEmpty(job.resources) ? null : job.resources.join(", ")],
                                                 ["Elastic Profile ID", job.elastic_profile_id],
                                                 ["Job Timeout", (this.jobTimeout(job))],
                                                 ["Run type", this.jobRunType(job)],
                                               ]);

    return (
      <div data-test-id={`selected-job-${stage.name}-${job.name}`} class={styles.stageOrJob}>
        Showing job <em>{stage.name}</em> > <em>{job.name}</em>
        <hr/>
        <KeyValuePair data={jobProperties}/>
        <Tabs
          tabs={["Tasks", "Artifacts", "Environment Variables", "Custom Tabs"]}
          contents={[this.tasks(job.tasks), this.artifacts(job.artifacts), this.environmentVariables(job.environment_variables), this.tabs(
            job.tabs)]}/>
      </div>
    );
  }

  private jobTimeout(job: Job) {
    let timeout: any;
    if (_.isNil(job.timeout)) {
      timeout = "Use server default";
    } else if (job.timeout === 0) {
      timeout = "Never timeout";
    } else {
      timeout = `Cancel after ${job.timeout} ${inflection.pluralize("minute", job.timeout)} of inactivity`;
    }
    return timeout;
  }

  private jobRunType(job: Job) {
    if (job.run_instance_count === "all") {
      return "Run on all agents";
    } else if (job.run_instance_count === 0) {
      return `Run on ${job.run_instance_count} agents`;
    } else {
      return `Run on 1 agent`;
    }
  }

  private yesOrNo(b: boolean) {
    return b ? "Yes" : "No";
  }

  private environmentVariables(variables: EnvironmentVariableJSON[]) {
    if (_.isEmpty(variables)) {
      return <FlashMessage message="No environment variables have been configured." type={MessageType.info}/>;
    }

    const data = new Map(variables.map((eachVar) => {
      return [eachVar.name, eachVar.secure ? "******" : eachVar.value];
    }));
    return <KeyValuePair data={data}/>;
  }

  private stagePermissions(stage: Stage) {
    const authorization = stage.approval.authorization;
    const data          = new Map<string, m.Children>();

    if (authorization) {
      if (authorization.users.length >= 1) {
        data.set("Users", authorization.users.join(", "));
      }
      if (authorization.roles.length >= 1) {
        data.set("Roles", authorization.roles.join(", "));
      }
    }

    if (data.size === 0) {
      return (
        <FlashMessage
          message="There are no operate permissions configured for this stage nor its pipeline group. All Go users can operate on this stage."
          type={MessageType.info}/>
      );
    } else {
      return <KeyValuePair data={data}/>;
    }

  }

  private artifacts(artifacts: Artifact[]) {
    if (_.isEmpty(artifacts)) {
      return (<FlashMessage message="No artifacts have been configured" type={MessageType.info}/>);
    }

    const artifactsGroupedByType = _.groupBy(artifacts,
                                             (eachArtifact) => eachArtifact.type);

    return [
      this.buildArtifacts(artifactsGroupedByType.build),
      this.testArtifacts(artifactsGroupedByType.test),
      this.externalArtifacts(artifactsGroupedByType.external),
    ];
  }

  private tabs(tabs: Tab[]) {
    if (_.isEmpty(tabs)) {
      return (<FlashMessage message="No custom tabs have been configured" type={MessageType.info}/>);
    }
    const data = tabs.map((eachTab) => {
      return [eachTab.name, eachTab.path];
    });

    return <Table headers={["Tab Name", "Path"]} data={data}/>;
  }

  private tasks(tasks: Task[]) {
    if (_.isEmpty(tasks)) {
      return (<FlashMessage message="No tasks have been configured" type={MessageType.info}/>);
    }

    return (
      <div class={styles.taskList}>
        {tasks.map((eachTask, index) => {
          return (
            <div data-test-id={`task-${index}`} class={styles.taskRow}>
              <div class={styles.taskDescription}>
                <TaskWidget pluginInfos={this.pluginInfos} task={eachTask}/>
              </div>
              <div class={styles.taskRunIf}>
                Run if {eachTask.attributes.run_if.join(", ")}
              </div>
            </div>
          );
        })}
      </div>
    );
  }

  private buildArtifacts(artifacts: Artifact[]) {
    if (_.isEmpty(artifacts)) {
      return <FlashMessage message="No build artifacts have been configured" type={MessageType.info}/>;
    }

    const data = artifacts.map((eachArtifact) => {
      return [eachArtifact.source, eachArtifact.destination];
    });

    return <Table caption="Build Artifacts" headers={["Source", "Destination"]} data={data}/>;
  }

  private testArtifacts(artifacts: Artifact[]) {
    if (_.isEmpty(artifacts)) {
      return <FlashMessage message="No test artifacts have been configured" type={MessageType.info}/>;
    }

    const data = artifacts.map((eachArtifact) => {
      return [eachArtifact.source, eachArtifact.destination];
    });

    return <Table caption="Test Artifacts" headers={["Source", "Destination"]} data={data}/>;
  }

  private externalArtifacts(artifacts: Artifact[]) {
    if (_.isEmpty(artifacts)) {
      return <FlashMessage message="No external artifacts have been configured" type={MessageType.info}/>;
    }

    return [
      <div>External Artifacts</div>,
      artifacts.map((eachArtifact) => {
        return this.externalArtifact(eachArtifact);
      })
    ];
  }

  private externalArtifact(artifact: Artifact) {
    const artifactInfo   = new Map([["Artifact ID", artifact.artifact_id], ["Store ID", artifact.store_id]]);
    const artifactConfig = new Map(artifact.configuration!.map((eachConfig) => {
      return [eachConfig.key, eachConfig.value || "******"];
    }));

    return (
      <div>
        <KeyValuePair data={artifactInfo}/>
        Configuration:
        <div style="padding-left: 15px;">
          <KeyValuePair data={artifactConfig}/>
        </div>
      </div>
    );
  }

}
