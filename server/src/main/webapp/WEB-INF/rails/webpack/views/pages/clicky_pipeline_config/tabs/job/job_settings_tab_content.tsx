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

import Awesomplete from "awesomplete";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ResourcesService} from "models/agents/agents_crud";
import {ElasticAgentProfilesCRUD} from "models/elastic_profiles/elastic_agent_profiles_crud";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {JobTimeoutManagementCRUD} from "models/server-configuration/server_configuartion_crud";
import {AutocompleteField, SuggestionProvider} from "views/components/forms/autocomplete";
import {NumberField, RadioField, TextField} from "views/components/forms/input_fields";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import styles from "./job_settings.scss";

export interface Attrs {
  entity: Job;
  readonly: boolean;
  resources: Stream<string[]>;
  defaultJobTimeout: Stream<number>;
  templateConfig: TemplateConfig;
  resourcesSuggestions: ResourcesSuggestionsProvider;
  elasticAgentsSuggestions: ElasticAgentSuggestionsProvider;
}

export class JobSettingsTabContentWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, this>) {
    const entity = vnode.attrs.entity;

    const numberFieldForTimeout = <div class={styles.numberFieldWrapper}>
      <NumberField property={entity.jobTimeoutType() !== "number" ? Stream() : (entity.timeout as any)}
                   errorText={entity.errors().errorsForDisplay("timeout")}
                   readonly={vnode.attrs.readonly || entity.jobTimeoutType() !== "number"}
                   dataTestId={"number-field-for-job-timeout"}/>
    </div>;

    const jobTimeoutInNumber: m.Child = <div class={styles.cancelAfterInactivityWrapper}>
      Cancel after {numberFieldForTimeout} minutes of inactivity
    </div>;

    const numberFieldForRunInstance = <div class={styles.numberFieldWrapper}>
      <NumberField property={entity.runType() !== "number" ? Stream() : (entity.runInstanceCount as any)}
                   errorText={entity.errors().errorsForDisplay("runInstanceCount")}
                   readonly={vnode.attrs.readonly || entity.runType() !== "number"}
                   dataTestId={"number-field-for-run-instance"}/>
    </div>;

    const runInstanceInNumber: m.Child = <div class={styles.cancelAfterInactivityWrapper}>
      Run {numberFieldForRunInstance} instances
    </div>;

    const timeout = vnode.attrs.defaultJobTimeout() === 0 ? "Never" : `${vnode.attrs.defaultJobTimeout()} minute(s)`;

    return <div data-test-id="job-settings-tab">
      <h3>Basic Settings</h3>
      <TextField required={true}
                 readonly={vnode.attrs.readonly}
                 errorText={entity.errors().errorsForDisplay("name")}
                 label="Job Name"
                 property={entity.name}/>
      <AutocompleteField label="Resources"
                         dataTestId={"resources-input"}
                         provider={vnode.attrs.resourcesSuggestions}
                         autoEvaluate={this.isResourcesInputOnFocus()}
                         errorText={entity.errors().errorsForDisplay("resources")}
                         replace={vnode.attrs.resourcesSuggestions.replace.bind(vnode.attrs.resourcesSuggestions)}
                         filter={vnode.attrs.resourcesSuggestions.filter.bind(vnode.attrs.resourcesSuggestions)}
                         onchange={vnode.attrs.resourcesSuggestions.update.bind(vnode.attrs.resourcesSuggestions)}
                         helpText="The agent resources that the current job requires to run. Specify multiple resources as a comma separated list."
                         readonly={vnode.attrs.readonly || !!entity.elasticProfileId()}
                         property={entity.resources}/>
      <AutocompleteField label="Elastic Agent Profile Id"
                         dataTestId={"elastic-agent-id-input"}
                         autoEvaluate={this.isElasticAgentIdInputOnFocus()}
                         errorText={entity.errors().errorsForDisplay("elasticProfileId")}
                         provider={vnode.attrs.elasticAgentsSuggestions}
                         readonly={vnode.attrs.readonly || !!entity.resources()}
                         helpText={<div>The Elastic Agent Profile that the current job requires to run. Visit <a href="/go/admin/elastic_agent_configurations" title="Elastic Agents Configurations">Elastic Agent Configurations</a> page to manage elastic agent profiles.</div>}
                         property={entity.elasticProfileId}/>
      <h3>Job Timeout</h3>
      <RadioField onchange={(val) => this.toggleJobTimeout((val as "never" | "default" | "number"), entity)}
                  dataTestId={"job-timout"}
                  readonly={vnode.attrs.readonly}
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
                      helpText: `Use the default job timeout specified globally: (${timeout})`
                    },
                    {
                      label: jobTimeoutInNumber,
                      value: "number",
                      helpText: "When the current job is inactive for more than the specified time period (in minutes), GoCD will cancel the job."
                    },
                  ]}>
      </RadioField>
      <h3>Run Type</h3>
      <RadioField property={entity.runType}
                  readonly={vnode.attrs.readonly}
                  onchange={(val) => this.toggleRunInstance((val as "one" | "all" | "number"), entity)}
                  dataTestId={"run-type"}
                  possibleValues={[
                    {
                      label: "Run on one instance",
                      value: "one",
                      helpText: "Job will run on the first available agent which matches its resources (if any) and are in the same environment as this job’s pipeline."
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

  private isResourcesInputOnFocus(): boolean {
    return document.activeElement?.getAttribute("data-test-id") === "resources-input";
  }

  private isElasticAgentIdInputOnFocus(): boolean {
    return document.activeElement?.getAttribute("data-test-id") === "elastic-agent-id-input";
  }

  private toggleJobTimeout(timeoutType: "never" | "default" | "number", entity: Job) {
    (timeoutType === "never")
      ? entity.timeout("never")
      : ((timeoutType === "default") ? entity.timeout(null) : entity.timeout(0));
  }

  private toggleRunInstance(timeoutType: "one" | "all" | "number", entity: Job) {
    (timeoutType === "all")
      ? entity.runInstanceCount("all")
      : ((timeoutType === "one") ? entity.runInstanceCount(null) : entity.runInstanceCount(0));
  }
}

export class JobSettingsTabContent extends TabContent<Job> {
  private readonly resources: Stream<string[]>       = Stream([] as string[]);
  private readonly elasticAgentIds: Stream<string[]> = Stream([] as string[]);
  private readonly defaultJobTimeout: Stream<number> = Stream();

  constructor() {
    super();
    this.fetchResources();
    this.fetchElasticAgents();
    this.fetchDefaultJobTimeout();
  }

  static tabName(): string {
    return "Job Settings";
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    const resourcesSuggestionsProvider     = new ResourcesSuggestionsProvider(entity.resources, this.resources);
    const elasticAgentsSuggestionsProvider = new ElasticAgentSuggestionsProvider(this.elasticAgentIds);

    return <JobSettingsTabContentWidget entity={entity}
                                        readonly={this.isEntityDefinedInConfigRepository()}
                                        resourcesSuggestions={resourcesSuggestionsProvider}
                                        elasticAgentsSuggestions={elasticAgentsSuggestionsProvider}
                                        defaultJobTimeout={this.defaultJobTimeout}
                                        resources={this.resources}
                                        templateConfig={templateConfig}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    const jobs = pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs();
    return Array.from(jobs).find(j => j.getOriginalName() === routeParams.job_name!)!;
  }

  private fetchResources() {
    new ResourcesService().all((data: string) => {
      this.resources(JSON.parse(data));
    });
  }

  private fetchElasticAgents() {
    ElasticAgentProfilesCRUD.all().then((elasticProfilesResponse) => {
      elasticProfilesResponse.do((successResponse) => {
        this.elasticAgentIds(successResponse.body.all()().map(p => p.id()) as string[]);
      });
    });
  }

  private fetchDefaultJobTimeout() {
    JobTimeoutManagementCRUD.get().then((jobTimeoutResponse) => {
      jobTimeoutResponse.do((successResponse) => {
        this.defaultJobTimeout(successResponse.body.defaultJobTimeout());
      });
    });
  }
}

export class ResourcesSuggestionsProvider extends SuggestionProvider {
  private allResources: Stream<string[]>;
  private property: Stream<string>;

  constructor(property: Stream<string>, allResources: Stream<string[]>) {
    super();
    this.property     = property;
    this.allResources = allResources;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve(this.allResources());
    });
  }

  replace(suggestion: any) {
    let updatedValues: string[] = [];
    if (this.property().trim().length > 0) {
      updatedValues = this.property().split(',');
      updatedValues.pop();
    }
    updatedValues.push(suggestion.value);
    this.property(updatedValues.join() + ",");
    m.redraw.sync();
  }

  filter(suggestion: any, input: string) {
    const definedResources = input.split(",").map(r => r.trim());
    return definedResources.every((r) => suggestion.value !== r);
  }
}

export class ElasticAgentSuggestionsProvider extends SuggestionProvider {
  private allElasticAgentIds: Stream<string[]>;

  constructor(allElasticAgentIds: Stream<string[]>) {
    super();
    this.allElasticAgentIds = allElasticAgentIds;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve(this.allElasticAgentIds());
    });
  }
}
