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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import s from "underscore.string";
import {Secondary} from "views/components/buttons";
import {Delete} from "views/components/icons";
import {Table} from "views/components/table";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {AddJobModal} from "views/pages/clicky_pipeline_config/tabs/stage/jobs/add_job_modal";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {OperationState} from "views/pages/page_operations";
import styles from "./jobs_tab_content.scss";

export class JobsTabContent extends TabContent<Stage> {
  private pipelineConfig?: PipelineConfig;
  private routeParams?: PipelineConfigRouteParams;
  private ajaxOperationMonitor?: Stream<OperationState>;

  static tabName(): string {
    return "Jobs";
  }

  public shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  content(pipelineConfig: PipelineConfig,
          templateConfig: TemplateConfig,
          routeParams: PipelineConfigRouteParams,
          ajaxOperationMonitor: Stream<OperationState>,
          save: () => Promise<any>,
          reset: () => void): m.Children {
    this.pipelineConfig       = pipelineConfig;
    this.routeParams          = routeParams;
    this.ajaxOperationMonitor = ajaxOperationMonitor;

    return super.content(pipelineConfig, templateConfig, routeParams, ajaxOperationMonitor, save, reset);
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!;
  }

  protected renderer(stage: Stage, templateConfig: TemplateConfig, save: () => Promise<any>, reset: () => void) {
    return [
      <JobsWidget jobs={stage.jobs}
                  stage={stage}
                  templateConfig={templateConfig}
                  pipelineConfig={this.pipelineConfig!}
                  routeParams={this.routeParams!}
                  ajaxOperationMonitor={this.ajaxOperationMonitor!}
                  pipelineConfigSave={save}
                  pipelineConfigReset={reset}
                  isEditable={true}/>
    ];
  }
}

export interface Attrs {
  jobs: Stream<NameableSet<Job>>;
  isEditable: boolean;
  stage: Stage;
  templateConfig: TemplateConfig;
  pipelineConfig: PipelineConfig;
  routeParams: PipelineConfigRouteParams;
  ajaxOperationMonitor: Stream<OperationState>;
  pipelineConfigSave: () => Promise<any>;
  pipelineConfigReset: () => void;
}

export interface State {
  getModal: () => AddJobModal;
}

export class JobsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.getModal = () => new AddJobModal(vnode.attrs.stage,
                                                 vnode.attrs.templateConfig,
                                                 vnode.attrs.pipelineConfig,
                                                 vnode.attrs.routeParams,
                                                 vnode.attrs.ajaxOperationMonitor,
                                                 vnode.attrs.pipelineConfigSave,
                                                 vnode.attrs.pipelineConfigReset);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    return <div data-test-id={"stages-container"}>
      <div class={styles.jobHelpText}>
        Manage jobs for this stage. All these jobs will be run in parallel (given sufficient matching agents), so they
        should not depend on each other.
      </div>

      <Table headers={JobsWidget.getTableHeaders(vnode.attrs.isEditable)}
             data={JobsWidget.getTableData(vnode.attrs.jobs(), vnode.attrs.isEditable)}
             draggable={false}
             dragHandler={JobsWidget.reArrange.bind(this, vnode.attrs.jobs)}/>
      <Secondary disabled={!vnode.attrs.isEditable}
                 dataTestId={"add-jobs-button"}
                 onclick={() => vnode.state.getModal().render()}>Add new job</Secondary>
    </div>;
  }

  private static getTableHeaders(isEditable: boolean) {
    const headers = ["Job", "Resources", "Run on all", "Run multiple instances"];
    if (isEditable) {
      headers.push("Remove");
    }
    return headers;
  }

  private static getTableData(jobs: NameableSet<Job>, isEditable: boolean): m.Child[][] {
    return Array.from(jobs.values()).map((job: Job) => {
      const runOnAllInstance    = job.runInstanceCount() === "all" ? "Yes" : "No";
      const runMultipleInstance = (typeof job.runInstanceCount() === "number") ? "Yes" : "No";
      const cells: m.Child[]    = [job.name(), job.resources(), runOnAllInstance, runMultipleInstance];
      if (isEditable) {
        cells.push(<Delete iconOnly={true} onclick={() => jobs.delete(job)}
                           data-test-id={`${s.slugify(job.name())}-delete-icon`}/>);
      }
      return cells;
    });
  }

  private static reArrange(jobs: Stream<NameableSet<Job>>, oldIndex: number, newIndex: number) {
    const array = Array.from(jobs().values());
    array.splice(newIndex, 0, array.splice(oldIndex, 1)[0]);
    jobs(new NameableSet(array));
  }
}
