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
import {FlashMessage, FlashMessageModel} from "views/components/flash_message";
import {Modal, ModalState, Size} from "views/components/modal";

export abstract class AbstractTaskModal extends Modal {
  readonly flashMessage: FlashMessageModel;
  private readonlyAttr: boolean;
  private onAdd: (t: Task) => Promise<any>;

  constructor(onAdd: (t: Task) => Promise<any>, readonly: boolean) {
    super(Size.medium);
    this.onAdd        = onAdd;
    this.readonlyAttr     = readonly;
    this.flashMessage = new FlashMessageModel();
  }

  abstract getTask(): Task;

  renderFlashMessage() {
    return <FlashMessage message={this.flashMessage.message} type={this.flashMessage.type}/>;
  }

  buttons(): m.ChildArray {
    if (this.readonlyAttr) {
      return [];
    }

    return [
      <Primary data-test-id="save-pipeline-group" onclick={this.addTaskAndSave.bind(this)}>Save</Primary>,
      <Cancel data-test-id="cancel-button" onclick={this.close.bind(this)}>Cancel</Cancel>
    ];
  }

  addTaskAndSave() {
    this.modalState = ModalState.LOADING;
    this.onAdd(this.getTask()).finally(() => this.modalState = ModalState.OK);
  }
}
