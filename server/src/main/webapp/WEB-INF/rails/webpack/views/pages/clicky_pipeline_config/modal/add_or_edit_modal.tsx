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
import Stream from "mithril/stream";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Job} from "models/pipeline_configs/job";
import {Stage} from "models/pipeline_configs/stage";
import * as Buttons from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {StageEditor} from "../widgets/stage_editor_widget";

export abstract class AddOrEditEntityModal<T extends ValidatableMixin> extends Modal {
  protected readonly entity: Stream<T>;
  private readonly __title: string;
  protected readonly onSuccessfulAdd: (entity: T) => void;

  protected constructor(title: string, entity: Stream<T>, onSuccessfulAdd: (entity: T) => void) {
    super(Size.large);
    this.__title         = title;
    this.entity          = entity;
    this.onSuccessfulAdd = onSuccessfulAdd;
  }

  title(): string {
    return this.__title;
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="button-ok" onclick={this.addOrUpdateEntity.bind(this)}>Save</Buttons.Primary>
    ];
  }

  addOrUpdateEntity(): void {
    if (this.entity().isValid()) {
      this.close();
      this.onSuccessfulAdd(this.entity());
    }
  }
}

export class StageModal extends AddOrEditEntityModal<Stage> {
  static forAdd(onSuccessfulAdd: (stage: Stage) => void) {
    const newStage = new Stage();
    newStage.jobs().add(new Job());
    return new StageModal("Add new stage", Stream(newStage), onSuccessfulAdd);
  }

  body() {
    return <StageEditor stage={this.entity}/>;
  }
}
