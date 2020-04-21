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

import {ErrorResponse} from "helpers/api_request_builder";
import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {NameableSet} from "models/pipeline_configs/nameable_set";
import {Stage} from "models/pipeline_configs/stage";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {SelectField, SelectFieldOptions, TextField} from "views/components/forms/input_fields";
import {Modal, ModalState} from "views/components/modal";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import styles from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content.scss";
import {StageSettingsWidget} from "views/pages/clicky_pipeline_config/tabs/stage/stage_settings_tab_content";

export class AddStageModal extends Modal {
  private stages: NameableSet<Stage>;
  private pipelineConfigSave: () => Promise<any>;

  private readonly selectedTaskTypeToAdd: Stream<string>;
  private readonly allTaskTypes: string[];

  private stageToCreate: Stage;
  private jobToCreate: Job;
  private taskModal: AbstractTaskModal | undefined;

  private readonly existingStageNames: string[];
  private readonly errorMsg = `Another stage with the same name already exists!`;

  constructor(stages: NameableSet<Stage>, pipelineConfigSave: () => Promise<any>) {
    super();

    this.stages             = stages;
    this.pipelineConfigSave = pipelineConfigSave;

    this.stageToCreate = new Stage();
    this.jobToCreate   = new Job();

    this.allTaskTypes          = ["Ant", "NAnt", "Rake", "Custom Command"];
    this.selectedTaskTypeToAdd = Stream(this.allTaskTypes[0]);

    this.existingStageNames = Array.from(this.stages.keys()).map(s => s.name());

    this.updateTaskModal();
  }

  onbeforeupdate(vnode: m.VnodeDOM<any, this>): any {
    this.validateStageNameUniqueness();
  }

  body(): m.Children {
    return <div data-test-id="add-stage-modal">
      <StageSettingsWidget stage={this.stageToCreate} isForAddStagePopup={true}/>
      <h3 data-test-id="initial-job-and-task-header">Initial Job and Task</h3>
      <div data-test-id="initial-job-and-task-header-help-text" className={styles.jobHelpText}>
        You can add more jobs and tasks to this stage once the stage has been created.
      </div>
      <TextField required={true}
                 errorText={this.jobToCreate.errors().errorsForDisplay("name")}
                 label="Job Name"
                 property={this.jobToCreate.name}/>
      <SelectField property={this.selectedTaskTypeToAdd}
                   label={"Task Type"}
                   onchange={this.updateTaskModal.bind(this)}>
        <SelectFieldOptions selected={this.selectedTaskTypeToAdd()}
                            items={this.allTaskTypes}/>
      </SelectField>
      {this.taskModal!.body()}
    </div>;
  }

  title(): string {
    return "Add new Stage";
  }

  buttons(): m.ChildArray {
    return [
      <Buttons.Primary data-test-id="save-job"
                       disabled={this.hasDuplicateNameError()}
                       onclick={this.onSave.bind(this)}>
        Save
      </Buttons.Primary>,
      <Buttons.Cancel data-test-id="button-cancel"
                      onclick={this.onClose.bind(this)}>
        Cancel
      </Buttons.Cancel>
    ];
  }

  private hasDuplicateNameError() {
    return this.stageToCreate.errors().hasError("name", this.errorMsg);
  }

  private validateStageNameUniqueness() {
    const duplicateStage = this.existingStageNames.find(s => {
      return this.stageToCreate.name().toLowerCase() === s.toLowerCase();
    });

    if (duplicateStage) {
      this.stageToCreate.errors().addIfDoesNotExists("name", this.errorMsg);
    } else if (!duplicateStage) {
      this.stageToCreate.errors().clearError("name", this.errorMsg);
    }
  }

  private updateTaskModal() {
    this.taskModal = TasksWidget.getTaskModal(this.selectedTaskTypeToAdd(),
                                              undefined,
                                              this.onSave.bind(this),
                                              false,
                                              new PluginInfos(),
                                              this.noOperation.bind(this),
                                              this.noOperation.bind(this))!;
  }

  private noOperation() {
    return Promise.resolve();
  }

  private onSave() {
    this.modalState = ModalState.LOADING;
    this.jobToCreate.tasks([this.taskModal!.getTask()]);
    this.stageToCreate.jobs(new NameableSet([this.jobToCreate]));
    this.stages.add(this.stageToCreate);

    this.performPipelineSave();
  }

  private onClose() {
    this.stages.delete(this.stageToCreate);
    this.close();
  }

  private onTaskSaveFailure(errorResponse?: ErrorResponse) {
    if (errorResponse) {
      const parsed = JSON.parse(errorResponse.body!);
      this.stageToCreate.consumeErrorsResponse(parsed.data);
    }

    this.stages.delete(this.stageToCreate);
    m.redraw.sync();
  }

  private performPipelineSave() {
    this.pipelineConfigSave()
        .then(this.close.bind(this))
        .catch((errorResponse?: ErrorResponse) => {
          this.modalState = ModalState.OK;
          this.onTaskSaveFailure(errorResponse);
        });
  }
}
