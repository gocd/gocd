/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {Stage} from "models/pipeline_configs/stage";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import * as Buttons from "views/components/buttons";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Modal, ModalState} from "views/components/modal";
import {JobSettingsTabContent} from "views/pages/clicky_pipeline_config/tabs/job/job_settings_tab_content";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import styles from "views/pages/clicky_pipeline_config/tabs/stage/jobs_tab_content.scss";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {OperationState} from "views/pages/page_operations";

export class AddJobModal extends Modal {
  private readonly stage: Stage;
  private readonly templateConfig: TemplateConfig;
  private readonly jobSettingsTabContent: JobSettingsTabContent;
  private readonly pipelineConfig: PipelineConfig;
  private readonly routeParams: PipelineConfigRouteParams;
  private readonly pipelineConfigSave: () => Promise<any>;
  private readonly ajaxOperationMonitor: Stream<OperationState>;
  private readonly flashMessage: FlashMessageModelWithTimeout;
  private readonly selectedTaskTypeToAdd: Stream<string>;
  private readonly allTaskTypes: string[];
  private taskModal: AbstractTaskModal | undefined;

  private readonly jobToCreate: Job;

  private readonly existingJobNames: string[];

  private readonly errorMsg = `Another job with the same name already exists!`;

  constructor(stage: Stage, templateConfig: TemplateConfig, pipelineConfig: PipelineConfig,
              routeParams: PipelineConfigRouteParams, ajaxOperationMonitor: Stream<OperationState>,
              flashMessage: FlashMessageModelWithTimeout,
              pipelineConfigSave: () => Promise<any>, pipelineConfigReset: () => void) {
    super();

    const self = this;

    this.jobToCreate = new Job();

    this.stage                = stage;
    this.templateConfig       = templateConfig;
    this.pipelineConfig       = pipelineConfig;
    this.routeParams          = routeParams;
    this.ajaxOperationMonitor = ajaxOperationMonitor;
    this.flashMessage         = flashMessage;
    this.pipelineConfigSave   = pipelineConfigSave;

    this.jobSettingsTabContent = new JobSettingsTabContent();

    // @ts-ignore
    // override selectedEntity method to return the newly created job as the bounded job.
    this.jobSettingsTabContent.selectedEntity = (p: PipelineConfig, r: PipelineConfigRouteParams): Job => {
      return self.jobToCreate;
    };

    this.allTaskTypes          = ["Ant", "NAnt", "Rake", "Custom Command"];
    this.selectedTaskTypeToAdd = Stream(this.allTaskTypes[3]);

    this.existingJobNames = Array.from(this.stage.jobs().keys()).map(j => j.name());

    this.updateTaskModal();
  }

  body(): m.Children {
    return <div data-test-id="add-job-modal">
      {this.jobSettingsTabContent.content(this.pipelineConfig,
                                          this.templateConfig,
                                          this.routeParams,
                                          this.ajaxOperationMonitor,
                                          this.flashMessage,
                                          this.noOperation.bind(this),
                                          this.noOperation.bind(this))}
      <h3 data-test-id="initial-task-header">Initial Task</h3>
      <div data-test-id="initial-task-help-text" class={styles.jobHelpText}>
        This job requires at least one task. You can add more tasks once this job has been created
      </div>
      <SelectField property={this.selectedTaskTypeToAdd}
                   onchange={this.updateTaskModal.bind(this)}>
        <SelectFieldOptions selected={this.selectedTaskTypeToAdd()}
                            items={this.allTaskTypes}/>
      </SelectField>
      {this.taskModal!.body()}
    </div>;
  }

  title(): string {
    return "Add new Job";
  }

  onbeforeupdate(vnode: m.VnodeDOM<any, this>): any {
    this.validateJobNameUniqueness();
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
    return this.jobToCreate.errors().hasError("name", this.errorMsg);
  }

  private validateJobNameUniqueness() {
    const duplicateJob = this.existingJobNames.find(j => {
      return this.jobToCreate.name().toLowerCase() === j.toLowerCase();
    });
    if (duplicateJob) {
      this.jobToCreate.errors().addIfDoesNotExists("name", this.errorMsg);
    } else if (!duplicateJob) {
      this.jobToCreate.errors().clearError("name", this.errorMsg);
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
    this.jobToCreate.tasks([]);
    this.jobToCreate.tasks().push(this.taskModal!.getTask());
    this.stage.jobs().add(this.jobToCreate);

    return this.performPipelineSave();
  }

  private onClose() {
    this.stage.jobs().delete(this.jobToCreate);
    this.close();
  }

  private onTaskSaveFailure(errorResponse?: string) {
    if (errorResponse) {
      const parsed = JSON.parse(JSON.parse(errorResponse).body);
      this.jobToCreate.consumeErrorsResponse(parsed.data);
    }

    this.flashMessage.clear();
    this.stage.jobs().delete(this.jobToCreate);
    m.redraw.sync();
  }

  private performPipelineSave() {
    return this.pipelineConfigSave()
               .then(this.close.bind(this))
               .catch((errorResponse?: string) => {
                 this.modalState = ModalState.OK;
                 this.onTaskSaveFailure(errorResponse);
               });
  }
}
