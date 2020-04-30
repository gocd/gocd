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

import {ApiRequestBuilder, ApiResult, ApiVersion, ErrorResponse} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {AbstractTask, Task} from "models/pipeline_configs/task";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Secondary} from "views/components/buttons";
import {FlashMessageModelWithTimeout, MessageType} from "views/components/flash_message";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Delete} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import {Table} from "views/components/table";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {AbstractTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/abstract";
import {AntTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/ant";
import {ExecTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/exec";
import {FetchArtifactTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch";
import {NantTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/nant";
import {PluggableTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/plugin";
import {RakeTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/rake";
import styles from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab.scss";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {OperationState} from "views/pages/page_operations";
import {ConfirmationDialog} from "views/pages/pipeline_activity/confirmation_modal";

export interface Attrs {
  tasks: Stream<Task[]>;
  isEditable: boolean;
  pluginInfos: Stream<PluginInfos>;
  autoSuggestions: Stream<any>;
  flashMessage: FlashMessageModelWithTimeout;
  pipelineConfigSave: () => Promise<any>;
  pipelineConfigReset: () => any;
}

export interface State {
  allTaskTypes: string[];
  selectedTaskTypeToAdd: Stream<string>;
  modal: AbstractTaskModal;
  entityReOrderHandler: EntityReOrderHandler;
}

export class TasksWidget extends MithrilComponent<Attrs, State> {
  static getTaskTypes(): Map<string, string> {
    return new Map([
                     ["ant", "Ant"],
                     ["nant", "NAnt"],
                     ["rake", "Rake"],
                     ["exec", "Custom Command"],
                     ["fetch", "Fetch Artifact"],
                     ["pluggable_task", "Plugin Task"]
                   ]);
  }

  static getTaskModal(type: string,
                      task: Task | undefined,
                      onSave: (t: Task) => Promise<any>,
                      showOnCancel: boolean,
                      pluginInfos: PluginInfos,
                      pipelineConfigSave: () => Promise<any>,
                      pipelineConfigReset: () => any,
                      readonly: boolean = false,
                      autoSuggestions?: Stream<any>) {
    switch (type) {
      case "Ant":
        return new AntTaskModal(task, showOnCancel, onSave, pluginInfos, readonly);
      case "NAnt":
        return new NantTaskModal(task, showOnCancel, onSave, pluginInfos, readonly);
      case "Rake":
        return new RakeTaskModal(task, showOnCancel, onSave, pluginInfos, readonly);
      case "Custom Command":
        return new ExecTaskModal(task, showOnCancel, onSave, pluginInfos, readonly);
      case "Plugin Task":
        return new PluggableTaskModal(task, showOnCancel, onSave, pluginInfos, readonly);
      case "Fetch Artifact":
        return new FetchArtifactTaskModal(task, showOnCancel, onSave, pluginInfos, readonly, autoSuggestions!);
      default:
        throw new Error("Unsupported Task Type!");
    }
  }

  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.allTaskTypes          = Array.from(TasksWidget.getTaskTypes().values());
    vnode.state.selectedTaskTypeToAdd = Stream(vnode.state.allTaskTypes[3]);
    vnode.state.entityReOrderHandler  = new EntityReOrderHandler("Task",
                                                                 vnode.attrs.flashMessage,
                                                                 vnode.attrs.pipelineConfigSave,
                                                                 vnode.attrs.pipelineConfigReset);
  }

  onTaskSave(vnode: m.Vnode<Attrs, State>): Promise<any> {
    vnode.attrs.tasks().push(vnode.state.modal.getTask());
    return this.performPipelineSave(vnode, true);
  }

  onTaskUpdate(vnode: m.Vnode<Attrs, State>, index: number, updated: Task) {
    const tasks  = vnode.attrs.tasks();
    tasks[index] = updated;
    vnode.attrs.tasks(tasks);

    return this.performPipelineSave(vnode, false);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    let addTaskView: m.Children;
    if (vnode.attrs.isEditable) {
      addTaskView = (<div className={styles.addTaskWrapper}>
        <SelectField property={vnode.state.selectedTaskTypeToAdd}>
          <SelectFieldOptions selected={vnode.state.selectedTaskTypeToAdd()}
                              items={vnode.state.allTaskTypes}/>
        </SelectField>
        <Secondary small={true} dataTestId={"add-task-button"}
                   onclick={() => {
                     vnode.state.modal = TasksWidget.getTaskModal(vnode.state.selectedTaskTypeToAdd(),
                                                                  undefined,
                                                                  this.onTaskSave.bind(this, vnode),
                                                                  true,
                                                                  vnode.attrs.pluginInfos(),
                                                                  vnode.attrs.pipelineConfigSave,
                                                                  vnode.attrs.pipelineConfigReset,
                                                                  !vnode.attrs.isEditable,
                                                                  vnode.attrs.autoSuggestions)!;

                     vnode.state.modal.render();
                   }}>
          Add Task
        </Secondary>
      </div>);
    }

    return <div data-test-id={"tasks-container"}>
      {vnode.state.entityReOrderHandler.getReOrderConfirmationView()}
      <Table headers={TasksWidget.getTableHeaders(vnode.attrs.isEditable)}
             draggable={vnode.attrs.isEditable}
             dragHandler={TasksWidget.reArrange.bind(this, vnode.attrs.tasks)}
             dragEnd={vnode.state.entityReOrderHandler.onReOder.bind(vnode.state.entityReOrderHandler)}
             data={this.getTableData(vnode)}/>
      {addTaskView}
    </div>;
  }

  private static reArrange(jobs: Stream<Task[]>, oldIndex: number, newIndex: number) {
    const array = Array.from(jobs());
    array.splice(newIndex, 0, array.splice(oldIndex, 1)[0]);
    jobs(array);
  }

  private static getTableHeaders(isEditable: boolean) {
    const headers = ["Task Type", "Run If Condition", "Properties", "On Cancel"];

    if (isEditable) {
      headers.push("Remove");
    }

    return headers;
  }

  private onTaskSaveFailure(vnode: m.Vnode<Attrs, State>,
                            shouldRemoveTaskOnFailure: boolean,
                            errorResponse?: ErrorResponse) {
    if (errorResponse) {
      const parsed = JSON.parse(errorResponse.body!);
      vnode.state.modal.getTask()!.consumeErrorsResponse(parsed.data);
      vnode.state.modal.flashMessage.setMessage(MessageType.alert, parsed.message);
      if (shouldRemoveTaskOnFailure) {
        vnode.attrs.tasks().pop();
      }
    }

    m.redraw.sync();
  }

  private performPipelineSave(vnode: m.Vnode<Attrs, State>, shouldRemoveTaskOnFailure: boolean) {
    return vnode.attrs.pipelineConfigSave()
                .then(vnode.state.modal.close.bind(vnode.state.modal))
                .catch((errorResponse?: ErrorResponse) => {
                  this.onTaskSaveFailure(vnode, shouldRemoveTaskOnFailure, errorResponse);
                });
  }

  private getTableData(vnode: m.Vnode<Attrs, State>) {
    const tasks = vnode.attrs.tasks();

    return tasks.map((task: Task, index: number) => {
      const cells: m.Child[] = [
        <Link onclick={() => {
          vnode.state.modal = TasksWidget.getTaskModal(TasksWidget.getTaskTypes().get(task.type)!,
                                                       AbstractTask.fromJSON(task.toJSON()),
                                                       this.onTaskUpdate.bind(this, vnode, index),
                                                       true,
                                                       vnode.attrs.pluginInfos(),
                                                       vnode.attrs.pipelineConfigSave,
                                                       vnode.attrs.pipelineConfigReset,
                                                       !vnode.attrs.isEditable,
                                                       vnode.attrs.autoSuggestions)!;

          vnode.state.modal.render();
        }}>
          <b>{task.description(vnode.attrs.pluginInfos())}</b>
        </Link>,
        <i>{task.attributes().runIf().join(", ")}</i>,
        <KeyValuePair inline={true} data={task.attributes().properties()}/>,
        task.attributes().onCancel()?.type || "No"
      ];

      let deleteDisabledMessage: string | undefined;
      if (tasks.length === 1) {
        deleteDisabledMessage = "Can not delete the only task from the job.";
      }

      if (vnode.attrs.isEditable) {
        cells.push(<Delete iconOnly={true}
                           disabled={!!deleteDisabledMessage}
                           title={deleteDisabledMessage}
                           data-test-id={`task-${index}-delete-icon`}
                           onclick={this.deleteTask.bind(this, vnode, task, index)}/>);
      }

      return cells;
    });
  }

  private deleteTask(vnode: m.Vnode<Attrs, State>, taskToDelete: Task, taskIndex: number) {
    new ConfirmationDialog(
      "Delete Task",
      <div>Do you want to delete the task at index '<em>{taskIndex + 1}</em>'?</div>,
      this.onDelete.bind(this, vnode, taskToDelete, taskIndex)
    ).render();
  }

  private onDelete(vnode: m.Vnode<Attrs, State>, taskToDelete: Task, taskIndex: number) {
    vnode.attrs.tasks().splice(taskIndex, 1);
    return vnode.attrs.pipelineConfigSave().then(() => {
      vnode.attrs.flashMessage.setMessage(MessageType.success, `Task deleted successfully.`);
    }).catch((errorResponse: ErrorResponse) => {
      vnode.attrs.tasks().splice(taskIndex, 0, taskToDelete);
      vnode.attrs.flashMessage.consumeErrorResponse(errorResponse);
    }).finally(m.redraw.sync);
  }
}

export class TasksTabContent extends TabContent<Job> {
  private readonly pluginInfos: Stream<PluginInfos> = Stream();
  private autoSuggestions: Stream<any>              = Stream();

  constructor() {
    super();
    this.fetchPluginInfos();
  }

  static tabName(): string {
    return "Tasks";
  }

  content(pipelineConfig: PipelineConfig,
          templateConfig: TemplateConfig,
          routeParams: PipelineConfigRouteParams,
          ajaxOperationMonitor: Stream<OperationState>,
          flashMessage: FlashMessageModelWithTimeout,
          save: () => Promise<any>,
          reset: () => any): m.Children {
    if (!this.autoSuggestions()) {
      if (this.isPipelineConfigView()) {
        this.fetchUpstreamPipelines(pipelineConfig.name(), routeParams.stage_name!);
      } else {
        this.autoSuggestions({});
      }
    }

    return super.content(pipelineConfig, templateConfig, routeParams, ajaxOperationMonitor, flashMessage, save, reset);
  }

  shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  protected renderer(entity: Job,
                     template: TemplateConfig,
                     flashMessage: FlashMessageModelWithTimeout,
                     save: () => Promise<any>,
                     reset: () => any): m.Children {
    return <TasksWidget pluginInfos={this.pluginInfos}
                        autoSuggestions={this.autoSuggestions}
                        pipelineConfigSave={save}
                        flashMessage={flashMessage}
                        pipelineConfigReset={reset}
                        tasks={entity.tasks}
                        isEditable={!this.isEntityDefinedInConfigRepository()}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }

  private fetchPluginInfos() {
    return PluginInfoCRUD.all({})
                         .then((pluginInfosResponse) => {
                           pluginInfosResponse.do((successResponse) => {
                             this.pluginInfos(successResponse.body);
                             this.pageLoaded();
                           }, super.pageLoadFailure);
                         });
  }

  private fetchUpstreamPipelines(pipelineName: string, stageName: string): any {
    return ApiRequestBuilder.GET(SparkRoutes.getUpstreamPipelines(pipelineName, stageName), ApiVersion.v1)
                            .then((result: ApiResult<string>) => {
                              return result.map((str) => {
                                return this.autoSuggestions(JSON.parse(str));
                              });
                            });
  }

}
