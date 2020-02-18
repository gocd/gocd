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
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import s from "underscore.string";
import {Secondary} from "views/components/buttons";
import {Delete} from "views/components/icons";
import {Table} from "views/components/table";
import {JobModal} from "views/pages/clicky_pipeline_config/modal/add_or_edit_modal";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export class JobsTabContent extends TabContent<Stage> {
  name() {
    return "Jobs";
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Stage {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!;
  }

  protected renderer(stage: Stage, templateConfig: TemplateConfig) {
    return [
      <JobsWidget jobs={stage.jobs} isEditable={true}/>
    ];
  }
}

export interface Attrs {
  jobs: Stream<NameableSet<Job>>;
  isEditable: boolean;
}

export class JobsWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id={"stages-container"}>
      <span>Manage jobs for this stage. All these jobs will be run in parallel (given sufficient matching agents), so they should not depend on each other.</span>
      <Table headers={JobsWidget.getTableHeaders(vnode.attrs.isEditable)}
             data={JobsWidget.getTableData(vnode.attrs.jobs(), vnode.attrs.isEditable)}
             draggable={vnode.attrs.isEditable}
             dragHandler={JobsWidget.reArrange.bind(this, vnode.attrs.jobs)}/>
      <Secondary disabled={!vnode.attrs.isEditable}
                 dataTestId={"add-stage-button"}
                 onclick={JobsWidget.showAddJobModal.bind(this, vnode.attrs.jobs)}>Add new job</Secondary>
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

  private static showAddJobModal(jobs: Stream<NameableSet<Job>>) {
    return JobModal.forAdd((updatedJob: Job) => {
      jobs().add(updatedJob);
    }).render();
  }
}
