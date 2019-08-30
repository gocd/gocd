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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import {ExecTask, Task, TaskType} from "models/new_pipeline_configs/task";
import {RadioField} from "views/components/forms/input_fields";
import {TaskEditor} from "views/pages/pipeline_configs/stages/on_create/task_editor_widget";
import styles from "./tasks.scss";

export interface TasksDescriptionAttrs {
  task: Task;
}

class ExecTaskDescriptionWidget extends MithrilViewComponent<TasksDescriptionAttrs> {
  view(vnode: m.Vnode<TasksDescriptionAttrs>) {
    // @ts-ignore
    const task = vnode.attrs.task as ExecTask;

    return <div data-test-id="exec-task-description">
      <TaskEditor task={task}/>
      <div data-test-id="run-if-conditions-container" class={styles.runIfConditionsWrapper}>
        <RadioField label="Run If Conditions"
                    property={(task).runIfCondition}
                    inline={true}
                    required={true}
                    possibleValues={[
                      {label: "Passed", value: "Passed"},
                      {label: "Failed", value: "Failed"},
                      {label: "Any", value: "Any"}
                    ]}>
        </RadioField>
      </div>
    </div>;
  }
}

class FetchArtifactTaskDescriptionWidget extends MithrilViewComponent<TasksDescriptionAttrs> {
  view(vnode: m.Vnode<TasksDescriptionAttrs>) {
    return <div data-test-id="fetch-task-description">
      i am representing a fetch artifact task..
    </div>;
  }
}

export class TaskDescriptionWidget extends MithrilViewComponent<TasksDescriptionAttrs> {
  view(vnode: m.Vnode<TasksDescriptionAttrs>) {
    const taskToDisplay: Task = vnode.attrs.task;

    let widget: any;
    switch (taskToDisplay.getType()) {
      case TaskType.Exec:
        widget = <ExecTaskDescriptionWidget {...vnode.attrs}/>;
        break;
      case TaskType.Fetch:
        widget = <FetchArtifactTaskDescriptionWidget {...vnode.attrs}/>;
        break;
      default:
        throw new Error(`Tasks of type '${taskToDisplay.getType()}' can not be rendered!`);
    }

    return <div class={styles.taskDescriptionContainer}>
      {widget}
    </div>;
  }
}
