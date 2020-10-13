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
import Stream from "mithril/stream";
import {ElasticAgentProfilesCRUD} from "models/elastic_profiles/elastic_agent_profiles_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {Form} from "views/components/forms/form";
import {CheckboxField, RadioField, TextField} from "views/components/forms/input_fields";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {ElasticAgentSuggestionsProvider} from "../job/job_settings_tab_content";

export class GeneralOptionsTabContent extends TabContent<PipelineConfig> {
  private readonly elasticAgentIds: Stream<string[]> = Stream([] as string[]);

  constructor() {
    super();
    this.fetchElasticAgents();
  }

  static tabName(): string {
    return "General";
  }

  getPipelineSchedulingCheckBox(entity: PipelineConfig, templateConfig: TemplateConfig) {
    let additionalHelpText: string = ";";
    if (entity.isUsingTemplate()) {
      additionalHelpText
        = ` Since this pipeline is based on '${entity.template()}' template, automatic/manual behaviour of the pipeline is determined by the template's first stage.`;
    }

    const stage: Stage = entity.template() ? templateConfig.firstStage() : entity.firstStage();
    if (stage) {
      return <CheckboxField label="Automatic pipeline scheduling"
                            errorText={entity.errors().errorsForDisplay("")}
                            dataTestId={"automatic-pipeline-scheduling"}
                            readonly={entity.isDefinedInConfigRepo() || entity.isUsingTemplate()}
                            helpText={`If unchecked, this pipeline will only schedule in response to a Manual/API/Timer trigger. Unchecking this box is the same as making the first stage manual.${additionalHelpText}`}
                            property={stage.approval().typeAsStream()}/>;
    }
  }

  protected renderer(entity: PipelineConfig, templateConfig: TemplateConfig): m.Children {
    return <div>
      <h3>Basic settings</h3>
      <Form compactForm={true}>
        <TextField property={entity.labelTemplate}
                   label={"Label Template"}
                   errorText={entity.errors().errorsForDisplay("labelTemplate")}
                   helpText={"Customize the label for this pipeline."}
                   docLink={"configuration/pipeline_labeling.html"}
                   placeholder={"${COUNT}"}
                   readonly={entity.isDefinedInConfigRepo()}
                   dataTestId={"label-template"}/>
        {this.getPipelineSchedulingCheckBox(entity, templateConfig)}
      </Form>

      <h3>Timer Settings</h3>
      <Form compactForm={true}>
        <TextField property={entity.timer().spec}
                   label={"Cron Timer Specification"}
                   errorText={entity.timer().errors().errorsForDisplay("spec")}
                   readonly={entity.isDefinedInConfigRepo()}
                   dataTestId={"cron-timer"}
                   helpText={"A cron-like schedule to build the pipeline. For example to run a pipeline once every night at 10 pm on weekdays, use '0 0 22 ? * MON-FRI'."}
                   docLink={"configuration/admin_timer.html"}/>
        <CheckboxField label="Run only on new material"
                       readonly={!entity.timer().spec() || entity.isDefinedInConfigRepo()}
                       dataTestId={"run-only-on-new-material"}
                       helpText="Run only if the pipeline hasn't previously run with the latest material(s). This option is typically useful when automatic pipeline scheduling is turned off. For this pipeline to schedule conditionally, please ensure at least one of its materials has polling enabled."
                       property={entity.timer().onlyOnChanges}/>
      </Form>

      <AutocompleteField label="Elastic Agent Profile Id"
                         dataTestId={"elastic-agent-id-input"}
                         errorText={entity.errors().errorsForDisplay("elasticProfileId")}
                         readonly={entity.isDefinedInConfigRepo()}
                         autoEvaluate={this.isElasticAgentIdInputOnFocus()}
                         provider={new ElasticAgentSuggestionsProvider(this.elasticAgentIds)}
                         helpText={<div>The Elastic Agent Profile that the current job requires to run. Visit <a
                           href="/go/admin/elastic_agent_configurations" title="Elastic Agents Configurations">Elastic Agent Configurations</a> page
                           to manage elastic agent profiles.</div>}
                         property={entity.elasticProfileId}/>

      <h3>Pipeline locking behavior</h3>
      <Form compactForm={true}>
        <RadioField property={entity.lockBehavior}
                    readonly={entity.isDefinedInConfigRepo()}
                    possibleValues={[
                      {
                        label:    "Run single instance of pipeline at a time",
                        value:    "unlockWhenFinished",
                        helpText: "Only a single instance of the pipeline will be run at a time and the pipeline will NOT be locked upon failure. The pipeline will only be locked to ensure a single instance, but will be unlocked if the pipeline finishes (irrespective of status) or reaches a manual stage."
                      },
                      {
                        label:    "Run single instance of pipeline and lock on failure",
                        value:    "lockOnFailure",
                        helpText: "Only a single instance of the pipeline will be run at a time and the pipeline will be locked upon failure. The pipeline can be unlocked manually and will be unlocked if it reaches the final stage, irrespective of the status of that stage. This is particularly useful in deployment scenarios."
                      },
                      {
                        label:    "Run multiple instances (default)",
                        value:    "none",
                        helpText: "This pipeline will not be locked and multiple instances of this pipeline will be allowed to run (default)."
                      },
                    ]}>
        </RadioField>
      </Form>
    </div>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): PipelineConfig {
    return pipelineConfig;
  }

  private isElasticAgentIdInputOnFocus(): boolean {
    return document.activeElement?.getAttribute("data-test-id") === "elastic-agent-id-input";
  }

  private fetchElasticAgents() {
    ElasticAgentProfilesCRUD.all().then((elasticProfilesResponse) => {
      elasticProfilesResponse.do((successResponse) => {
        this.elasticAgentIds(successResponse.body.all()().map(p => p.id()) as string[]);
      });
    });
  }
}
