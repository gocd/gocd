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
import Stream from "mithril/stream";
import {Task} from "models/new_pipeline_configs/task";
import {Tasks} from "models/new_pipeline_configs/tasks";
import * as Icons from "views/components/icons";
import styles from "views/pages/pipeline_configs/stages/on_create/tasks.scss";

export interface TasksAttrs {
  task: Task;
  index: number;
  tasks: Stream<Tasks>;
  selectedTaskIndex: Stream<number>;
}

class TaskWidget extends MithrilViewComponent<TasksAttrs> {
  view(vnode: m.Vnode<TasksAttrs>) {
    const isSelected = vnode.attrs.index === vnode.attrs.selectedTaskIndex();
    return <div
      data-test-id={isSelected ? "selected-task" : undefined}
      class={`${styles.taskItemWrapper} ${isSelected ? styles.selectedTask : undefined}`}
      onclick={vnode.attrs.selectedTaskIndex.bind(vnode.attrs, vnode.attrs.index)}>
      <div data-test-id="task-representation" class={styles.taskItem}>{vnode.attrs.task.represent()}</div>
      <div class={styles.deleteTaskWrapper}>
        <Icons.Delete iconOnly={true} onclick={(e: MouseEvent) => {
          e.stopPropagation();
          vnode.attrs.tasks().remove(vnode.attrs.task);
        }}/>
      </div>
    </div>;
  }
}

export interface TasksListAttrs {
  tasks: Stream<Tasks>;
  selectedTaskIndex: Stream<number>;
}

export class TasksListWidget extends MithrilViewComponent<TasksListAttrs> {
  view(vnode: m.Vnode<TasksListAttrs>) {
    return <div class={styles.tasksListContainer} data-test-id="tasks-list">
      {
        vnode.attrs.tasks().list().map((t, i) => {
          return <TaskWidget task={t} index={i}
                             tasks={vnode.attrs.tasks}
                             selectedTaskIndex={vnode.attrs.selectedTaskIndex}/>;
        })
      }
    </div>;
  }
}
