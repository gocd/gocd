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

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Tasks} from "models/new_pipeline_configs/tasks";
import {TasksListWidget} from "views/pages/pipeline_configs/stages/on_create/tasks_list_widget";
import {TaskDescriptionWidget} from "./task_description_widget";
import styles from "./tasks.scss";

export interface State {
  selectedTaskIndex: Stream<number>;
}

interface Attrs {
  tasks: Stream<Tasks>;
}

export class TasksTab extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {

    vnode.state.selectedTaskIndex = Stream(0);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const taskToRepresent = vnode.attrs.tasks().list()[vnode.state.selectedTaskIndex()];

    //todo: fix the data test ids for the tasks as the tasks are unique per job
    //may be change the data test id to include pipeline-stage-job name
    //Also, add a data-test-class for testing purpose!!
    return <div class={styles.tasksContainer} data-test-id="tasks-tab">
      <TasksListWidget tasks={vnode.attrs.tasks}
                       key={vnode.attrs.tasks.toString()}
                       selectedTaskIndex={vnode.state.selectedTaskIndex}/>
      <TaskDescriptionWidget key={taskToRepresent.represent()}
                             task={taskToRepresent}/>
    </div>;
  }
}
