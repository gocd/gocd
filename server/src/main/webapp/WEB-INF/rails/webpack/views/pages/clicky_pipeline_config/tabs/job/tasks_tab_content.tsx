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
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Task} from "models/pipeline_configs/task";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {Delete} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Table} from "views/components/table";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";

export interface Attrs {
  tasks: Stream<Task[]>;
  isEditable: boolean;
}

class TasksWidget extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div data-test-id={"tasks-container"}>
      <Table headers={TasksWidget.getTableHeaders(vnode.attrs.isEditable)}
             draggable={vnode.attrs.isEditable}
             dragHandler={TasksWidget.reArrange.bind(this, vnode.attrs.tasks)}
             data={TasksWidget.getTableData(vnode.attrs.tasks(), vnode.attrs.isEditable)}/>
    </div>;
  }

  private static reArrange(jobs: Stream<Task[]>, oldIndex: number, newIndex: number) {
    const array = Array.from(jobs());
    array.splice(newIndex, 0, array.splice(oldIndex, 1)[0]);
    jobs(array);
  }

  private static getTableHeaders(isEditable: boolean) {
    const headers = ["Task Type", "Run If Condition", "Properties", "On Cancel"];

    if (isEditable) {
      headers.push("Remove");
    }

    return headers;
  }

  private static getTableData(tasks: Task[], isEditable: boolean): m.Child[][] {
    return tasks.map((task: Task, index: number) => {
      const cells: m.Child[] = [
        <b>{task.type}</b>,
        <i>{task.attributes().runIf().join(", ")}</i>,
        <KeyValuePair inline={true} data={task.attributes().properties()}/>,
        task.attributes().onCancel()?.type || "No"
      ];

      if (isEditable) {
        cells.push(<Delete iconOnly={true}
                           onclick={() => tasks.splice(index, 1)}/>);
      }
      return cells;
    });
  }
}

export class TasksTabContent extends TabContent<Job> {
  name(): string {
    return "Tasks";
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    return <TasksWidget tasks={entity.tasks} isEditable={true}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }
}
