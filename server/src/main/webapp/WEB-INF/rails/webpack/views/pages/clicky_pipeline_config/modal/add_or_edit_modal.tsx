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
import {Material} from "models/materials/types";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Job} from "models/pipeline_configs/job";
import {Stage} from "models/pipeline_configs/stage";
import s from "underscore.string";
import * as Buttons from "views/components/buttons";
import {Modal, Size} from "views/components/modal";
import {JobEditor} from "views/pages/clicky_pipeline_config/widgets/job_editor_widget";
import {MaterialEditor} from "views/pages/pipelines/material_editor";
import {TaskTerminalField} from "views/pages/pipelines/task_editor";
import style from "../index.scss";
import {StageEditor} from "../widgets/stage_editor_widget";

abstract class AddOrEditEntityModal<T extends ValidatableMixin> extends Modal {
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

export class MaterialModal extends AddOrEditEntityModal<Material> {
  static forAdd(onSuccessfulAdd: (material: Material) => void) {
    return new MaterialModal("Add material", Stream(new Material("git")), onSuccessfulAdd);
  }

  static forEdit(material: Material, onSuccessfulAdd: (material: Material) => void) {
    const title          = `Edit material - ${s.capitalize(material.type()!)}`;
    const copyOfMaterial = Stream(new Material(material.type(), material.attributes()));
    return new MaterialModal(title, copyOfMaterial, onSuccessfulAdd);
  }

  body(): m.Children {
    return <MaterialEditor material={this.entity()}/>;
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

export class JobModal extends AddOrEditEntityModal<Job> {
  static forAdd(onSuccessfulAdd: (job: Job) => void) {
    const newJob = new Job();
    return new JobModal("Add new job", Stream(newJob), onSuccessfulAdd);
  }

  body() {
    return [
      <JobEditor job={this.entity()}/>,
      <h3>Initial Task</h3>,
      <span class={style.help}>This job requires at least one task. You can add more tasks once this job has been created.</span>,
      <TaskTerminalField label="Type your tasks below at the prompt"
                         property={this.entity().tasks}
                         errorText={this.entity().errors().errorsForDisplay("tasks")} required={true}/>
    ];
  }
}
