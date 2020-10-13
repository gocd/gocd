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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ElasticAgentProfilesCRUD} from "models/elastic_profiles/elastic_agent_profiles_crud";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {AutocompleteField} from "views/components/forms/autocomplete";
import {TextField} from "views/components/forms/input_fields";
import {SwitchBtn} from "views/components/switch";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {ElasticAgentSuggestionsProvider} from "../job/job_settings_tab_content";
import styles from "./stage_settings.scss";

export class StageSettingsTabContent extends TabContent<Stage> {
  private readonly elasticAgentIds: Stream<string[]> = Stream([] as string[]);
  private elasticAgentsSuggestionsProvider: ElasticAgentSuggestionsProvider | undefined;

  constructor() {
    super();
    this.fetchElasticAgents();
  }

  static tabName(): string {
    return "Stage Settings";
  }

  protected renderer(stage: Stage, templateConfig: TemplateConfig) {
    if (!this.elasticAgentsSuggestionsProvider) {
      this.elasticAgentsSuggestionsProvider = new ElasticAgentSuggestionsProvider(this.elasticAgentIds);
    }
    return <StageSettingsWidget stage={stage}
                                elasticAgentsSuggestions={this.elasticAgentsSuggestionsProvider}
                                readonly={this.isEntityDefinedInConfigRepository()}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return Array.from(pipelineConfig.stages()).find(s => s.getOriginalName() === routeParams.stage_name!)!;
  }

  private fetchElasticAgents() {
    ElasticAgentProfilesCRUD.all().then((elasticProfilesResponse) => {
      elasticProfilesResponse.do((successResponse) => {
        this.elasticAgentIds(successResponse.body.all()().map(p => p.id()) as string[]);
      });
    });
  }
}

interface Attrs {
  stage: Stage;
  readonly: boolean;
  //to reuse this view while creating a new stage. Based on this field, the view will not render less-important fields in create new stage view.
  isForAddStagePopup?: boolean;
  elasticAgentsSuggestions?: ElasticAgentSuggestionsProvider;
}

export class StageSettingsWidget extends MithrilViewComponent<Attrs> {
  static readonly APPROVAL_TYPE_HELP = "If turned on, then this stage will automatically schedule once the preceding stage completes successfully. Otherwise, users have to manually trigger this stage. For the first stage in a pipeline, turning this on is the same as checking 'Automatic Pipeline Scheduling' on the pipeline config.";

  static readonly ALLOW_ONLY_ON_SUCCESS_HELP = "Only allow stage to be scheduled if the previous stage run is successful.";

  view(vnode: m.Vnode<Attrs>) {
    const stage = vnode.attrs.stage;

    let additionalStageSettings: m.Children;
    let elasticProfileSettings: m.Children;
    if (!vnode.attrs.isForAddStagePopup) {
      elasticProfileSettings = <AutocompleteField label="Elastic Agent Profile Id"
                                                  dataTestId={"elastic-agent-id-input"}
                                                  autoEvaluate={this.isElasticAgentIdInputOnFocus()}
                                                  errorText={stage.errors().errorsForDisplay("elasticProfileId")}
                                                  onchange={vnode.attrs.elasticAgentsSuggestions!.update.bind(vnode.attrs.elasticAgentsSuggestions)}
                                                  provider={vnode.attrs.elasticAgentsSuggestions!}
                                                  readonly={vnode.attrs.readonly}
                                                  helpText={<div>The Elastic Agent Profile that the current job requires to run. Visit <a
                                                    href="/go/admin/elastic_agent_configurations" title="Elastic Agents Configurations">Elastic Agent
                                                    Configurations</a> page
                                                    to manage elastic agent profiles.</div>}
                                                  property={stage.elasticProfileId}/>;

      additionalStageSettings = <div data-test-id="additional-stage-settings">
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Fetch materials"
                     helpText="Perform material updates or checkouts."
                     dataTestId="fetch-materials-checkbox"
                     disabled={vnode.attrs.readonly}
                     small={true}
                     field={stage.fetchMaterials}/>
        </div>
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Never cleanup artifacts"
                     helpText="Never cleanup artifacts for this stage, if purging artifacts is configured at the Server Level."
                     dataTestId="never-cleanup-artifacts-checkbox"
                     small={true}
                     disabled={vnode.attrs.readonly}
                     field={stage.neverCleanupArtifacts}/>
        </div>
        <div className={styles.switchWrapper}>
          <SwitchBtn label="Clean Working Directory"
                     helpText="Remove all files/directories in the working directory on the agent."
                     dataTestId="clean-working-directory-checkbox"
                     small={true}
                     disabled={vnode.attrs.readonly}
                     field={stage.cleanWorkingDirectory}/>
        </div>
      </div>;
    }

    return <div data-test-id="stage-settings">
      <TextField label="Stage name"
                 required={true}
                 readonly={vnode.attrs.readonly}
                 dataTestId="stage-name-input"
                 errorText={stage.errors().errorsForDisplay("name")}
                 property={stage.name}/>
      {elasticProfileSettings}
      <div class={styles.switchWrapper}>
        <SwitchBtn label="Trigger on completion of previous stage"
                   helpText={StageSettingsWidget.APPROVAL_TYPE_HELP}
                   field={stage.approval().typeAsStream()}
                   disabled={vnode.attrs.readonly}
                   small={true}
                   dataTestId="approval-checkbox"
                   onclick={StageSettingsWidget.approvalChange.bind(this, stage)}/>
      </div>
      <div class={styles.switchWrapper}>
        <SwitchBtn label="Allow only on success of previous stage"
                   helpText={StageSettingsWidget.ALLOW_ONLY_ON_SUCCESS_HELP}
                   small={true}
                   disabled={vnode.attrs.readonly}
                   dataTestId="allow-only-on-success-checkbox"
                   field={stage.approval().allowOnlyOnSuccess}/>
      </div>
      {additionalStageSettings}
    </div>;
  }

  private static approvalChange(stage: Stage, e: MouseEvent) {
    const checkbox = e.currentTarget as HTMLInputElement;
    stage.approval().typeAsStream()(checkbox.checked);
  }

  private isElasticAgentIdInputOnFocus(): boolean {
    return document.activeElement?.getAttribute("data-test-id") === "elastic-agent-id-input";
  }
}
