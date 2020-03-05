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

import m from "mithril";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {NumberField, RadioField, TextField} from "views/components/forms/input_fields";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import styles from "./job_settings.scss";

export class JobSettingsTabContent extends TabContent<Job> {
  name(): string {
    return "Job Settings";
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    // @ts-ignore
    const numberFieldForTimeout = <div class={styles.numberFieldWrapper}><NumberField property={entity.timeout} readonly={entity.jobTimeoutType() !== "number"}/></div>
    const jobTimeoutInNumber: m.Child = <div class={styles.cancelAfterInactivityWrapper}>Cancel after {numberFieldForTimeout} minutes of inactivity</div>;

    // @ts-ignore
    const numberFieldForRunInstance = <div class={styles.numberFieldWrapper}><NumberField property={entity.runInstanceCount} readonly={entity.runType() !== "number"}/></div>
    const runInstanceInNumber: m.Child = <div class={styles.cancelAfterInactivityWrapper}>Run {numberFieldForRunInstance} instances</div>;

    return <div data-test-id="job-settings-tab">
      <TextField required={true}
                 label="Job Name"
                 property={entity.name}/>
      <TextField label="Resources"
                 helpText="The agent resources that the current job requires to run. Specify multiple resources as a comma separated list"
                 readonly={!!entity.elasticProfileId()}
                 property={entity.resources}/>
      <TextField label="Elastic Agent Profile Id"
                 readonly={!!entity.resources()}
                 helpText="The Elastic Agent Profile that the current job requires to run"
                 property={entity.elasticProfileId}/>
      <RadioField label="Job Timeout"
                  onchange={(val) => this.toggleJobTimeout((val as "never" | "default" | "number"), entity)}
                  property={entity.jobTimeoutType}
                  possibleValues={[
                    {
                      label: "Never",
                      value: "never",
                      helpText: "Never cancel the job."
                    },
                    {
                      label: "Use Default",
                      value: "default",
                      helpText: "Use the default job timeout specified globally."
                    },
                    {
                      label: jobTimeoutInNumber,
                      value: "number",
                      helpText: "When the current job is inactive for more than the specified time period (in minutes), GoCD will cancel the job."
                    },
                  ]}>
      </RadioField>

      <RadioField label="Run Type"
                  property={entity.runType}
                  onchange={(val) => this.toggleRunInstance((val as "one" | "all" | "number"), entity)}
                  possibleValues={[
                    {
                      label: "Run on one instance",
                      value: "one",
                      helpText: "Job will run on only agent that match its resources (if any) and are in the same environment as this job’s pipeline."
                    },
                    {
                      label: "Run on all agents",
                      value: "all",
                      helpText: "Job will run on all agents that match its resources (if any) and are in the same environment as this job’s pipeline. This option is particularly useful when deploying to multiple servers."
                    },
                    {
                      label: runInstanceInNumber,
                      value: "number",
                      helpText: "Specified number of instances of job will be created during schedule time."
                    },
                  ]}>
      </RadioField>
    </div>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }

  private toggleJobTimeout(timeoutType: "never" | "default" | "number", entity: Job) {
    (timeoutType === "never")
      ? entity.timeout("never")
      : ((timeoutType === "default") ? entity.timeout(null) : entity.timeout(entity.timeout()));
  }

  private toggleRunInstance(timeoutType: "one" | "all" | "number", entity: Job) {
    (timeoutType === "all")
      ? entity.runInstanceCount("all")
      : ((timeoutType === "one") ? entity.runInstanceCount(null) : entity.timeout(entity.timeout()));
  }
}
