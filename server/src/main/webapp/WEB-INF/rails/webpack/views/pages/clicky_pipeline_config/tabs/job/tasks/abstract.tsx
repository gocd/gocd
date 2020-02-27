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
import {Task} from "models/pipeline_configs/task";
import {Cancel, Primary} from "views/components/buttons";
import {Modal, Size} from "views/components/modal";

export abstract class AbstractTaskModal extends Modal {
  private onAdd: (t: Task) => void;

  constructor(onAdd: (t: Task) => void) {
    super(Size.medium);
    this.onAdd = onAdd;
  }

  abstract getTask(): Task;

  buttons(): m.ChildArray {
    return [
      <Primary data-test-id="save-pipeline-group" onclick={this.addTaskAndSave.bind(this)}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)}>Cancel</Cancel>
    ];
  }

  addTaskAndSave() {
    this.onAdd(this.getTask());
    this.close();
  }
}
