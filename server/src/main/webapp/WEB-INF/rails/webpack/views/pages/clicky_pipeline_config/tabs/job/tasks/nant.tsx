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
import {NantTask, Task} from "models/pipeline_configs/task";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";

export class NantTaskModal extends AbstractTaskModal {
  private readonly showOnCancel: boolean;

  constructor(task: Task | undefined, showOnCancel: boolean, onAdd: (t: Task) => void) {
    super(onAdd);

    this.showOnCancel = showOnCancel;
  }

  body(): m.Children {
    return <div>
      this will return nant task
      {this.showOnCancel}
    </div>;
  }

  title(): string {
    return "NAnt Task";
  }

  getTask(): Task {
    return new NantTask(undefined, undefined, undefined, undefined, []);
  }
}
