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
import {PipelineConfig} from "models/new_pipeline_configs/pipeline_config";
import * as Buttons from "views/components/buttons";
import {Form} from "views/components/forms/form";
import {CheckboxField, HelpText, RadioField, TextField} from "views/components/forms/input_fields";
import {Modal, Size} from "views/components/modal";
import {Tabs} from "views/components/tab";
import {EnvironmentVariablesEditor} from "views/pages/pipeline_configs/environment_variables_editor";
import {ParametersEditor} from "views/pages/pipeline_configs/parameters_editor";
import styles from "./pipeline_settings_modal.scss";

export class PipelineSettingsModal extends Modal {

  private readonly pipelineConfig: PipelineConfig;

  constructor(pipelineConfig: PipelineConfig) {
    super(Size.large);
    this.pipelineConfig = pipelineConfig;
  }

  body(): m.Children {
    return <Tabs tabs={["Basic Settings", "Environment Variables", "Parameters", "Project Management"]}
                 contents={[
                   this.basicSettings(),
                   this.environmentVariables(),
                   this.parameters(),
                   <p>Project Management</p>
                 ]}/>;

  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.close.bind(this)}>Save</Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel" onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
    ];
  }

  title(): string {
    return "Pipeline Settings";
  }

  private basicSettings(): m.Children {
    return <Form compactForm={true}>
      <div class={styles.formSection}>
        <TextField required={true}
                   property={this.pipelineConfig.labelTemplate}
                   label="Label Template"
                   helpText="Customize the label for this pipeline"
                   docLink={"configuration/pipeline_labeling.html"}
        />
        <CheckboxField
          helpText="If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual."
          label="Automatic Pipeline Scheduling"
          property={this.pipelineConfig.automaticScheduling}/>
      </div>
      <div class={styles.formSection}>
        <h2>Timer Settings</h2>
        <TextField required={true}
                   helpText="A cron-like schedule to build the pipeline. For example to run a pipeline once every night at 10 pm on weekdays, use '0 0 22 ? * MON-FRI'."
                   docLink={"/configuration/configuration_reference.html#timer"}
                   label="Cron Timer Specification"
                   property={this.pipelineConfig.timerSpecification}/>
        <CheckboxField
          helpText="Run only if the pipeline hasn't previously run with the latest material(s). This option is typically useful when automatic pipeline scheduling is turned off."
          label="Run only on new material"
          property={this.pipelineConfig.runOnNewMaterial}/>
      </div>
      <div class={styles.formSection}>
        <div class={styles.formSectionHeader}>
          <h2>Pipeline Locking Behaviour</h2>
          <HelpText helpTextId="help-pipeline-locking"
                    helpText="Ensure only one instance of a GoCD pipeline can run at the same time"
                    docLink="configuration/admin_lock_pipelines.html"/>
        </div>

        <RadioField property={this.pipelineConfig.lockingBehaviour}
                    possibleValues={[
                      {
                        label: "Run single instance of pipeline at a time",
                        value: "single_instance",
                        helpText: "Only a single instance of the pipeline will be run at a time and the pipeline will NOT be locked upon failure. The pipeline will only be locked to ensure a single instance, but will be unlocked if the pipeline finishes (irrespective of status) or reaches a manual stage."
                      },
                      {
                        label: "Run single instance of pipeline and lock on failure",
                        value: "single_instance_lock_on_failure",
                        helpText: "Only a single instance of the pipeline will be run at a time and the pipeline will be locked upon failure. The pipeline can be unlocked manually and will be unlocked if it reaches the final stage, irrespective of the status of that stage. This is particularly useful in deployment scenarios."
                      },
                      {
                        label: "Run multiple instances (default)",
                        value: "multiple_instances",
                        helpText: "This pipeline will not be locked and multiple instances of this pipeline will be allowed to run (default)."
                      },
                    ]}/>
      </div>
    </Form>;
  }

  private environmentVariables(): m.Children {
    return <EnvironmentVariablesEditor variables={this.pipelineConfig.environmentVariables}/>;
  }

  private parameters(): m.Children {
    return <ParametersEditor parameters={this.pipelineConfig.parameters}/>;
  }
}
