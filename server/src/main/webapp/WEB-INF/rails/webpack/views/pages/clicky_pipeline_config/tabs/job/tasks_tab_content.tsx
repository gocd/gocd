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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {Job} from "models/pipeline_configs/job";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {AbstractTask, Task} from "models/pipeline_configs/task";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {PluginInfoCRUD} from "models/shared/plugin_infos_new/plugin_info_crud";
import {Secondary} from "views/components/buttons";
import {SelectField, SelectFieldOptions} from "views/components/forms/input_fields";
import {Delete} from "views/components/icons";
import {KeyValuePair} from "views/components/key_value_pair";
import {Link} from "views/components/link";
import {Table} from "views/components/table";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {AntTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/ant";
import {ExecTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/exec";
import {FetchArtifactTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/fetch";
import {NantTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/nant";
import {PluggableTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/plugin";
import {RakeTaskModal} from "views/pages/clicky_pipeline_config/tabs/job/tasks/rake";
import styles from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab.scss";
import {TabContent} from "views/pages/clicky_pipeline_config/tabs/tab_content";
import {OperationState} from "views/pages/page_operations";

export interface Attrs {
  tasks: Stream<Task[]>;
  isEditable: boolean;
  pluginInfos: Stream<PluginInfos>;
  autoSuggestions: Stream<any>;
}

export interface State {
  allTaskTypes: string[];
  selectedTaskTypeToAdd: Stream<string>;
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
                      onSave: (t: Task) => void,
                      showOnCancel: boolean,
                      pluginInfos: PluginInfos,
                      autoSuggestions: Stream<any>) {
    switch (type) {
      case "Ant":
        return new AntTaskModal(task, showOnCancel, onSave, pluginInfos);
      case "NAnt":
        return new NantTaskModal(task, showOnCancel, onSave, pluginInfos);
      case "Rake":
        return new RakeTaskModal(task, showOnCancel, onSave, pluginInfos);
      case "Custom Command":
        return new ExecTaskModal(task, showOnCancel, onSave, pluginInfos);
      case "Plugin Task":
        return new PluggableTaskModal(task, showOnCancel, onSave, pluginInfos);
      case "Fetch Artifact":
        return new FetchArtifactTaskModal(task, showOnCancel, onSave, pluginInfos, autoSuggestions);
      default:
        throw new Error("Unsupported Task Type!");
    }
  }

  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.allTaskTypes          = Array.from(TasksWidget.getTaskTypes().values());
    vnode.state.selectedTaskTypeToAdd = Stream(vnode.state.allTaskTypes[0]);
  }

  onTaskAdd(vnode: m.Vnode<Attrs, State>, task: Task) {
    vnode.attrs.tasks().push(task);
  }

  view(vnode: m.Vnode<Attrs, State>) {
    return <div data-test-id={"tasks-container"}>
      <Table headers={TasksWidget.getTableHeaders(vnode.attrs.isEditable)}
             draggable={vnode.attrs.isEditable}
             dragHandler={TasksWidget.reArrange.bind(this, vnode.attrs.tasks)}
             data={TasksWidget.getTableData(vnode.attrs.pluginInfos(),
                                            vnode.attrs.autoSuggestions,
                                            vnode.attrs.tasks,
                                            vnode.attrs.isEditable)}/>
      <div className={styles.addTaskWrapper}>
        <SelectField property={vnode.state.selectedTaskTypeToAdd}>
          <SelectFieldOptions selected={vnode.state.selectedTaskTypeToAdd()}
                              items={vnode.state.allTaskTypes}/>
        </SelectField>
        <Secondary small={true}
                   onclick={() => TasksWidget.getTaskModal(vnode.state.selectedTaskTypeToAdd(),
                                                           undefined,
                                                           this.onTaskAdd.bind(this, vnode),
                                                           true,
                                                           vnode.attrs.pluginInfos(),
                                                           vnode.attrs.autoSuggestions)!.render()}>
          Add Task
        </Secondary>
      </div>
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

  private static getTableData(pluginInfos: PluginInfos,
                              autoSuggestions: Stream<any>,
                              tasks: Stream<Task[]>,
                              isEditable: boolean): m.Child[][] {

    return tasks().map((task: Task, index: number) => {
      const cells: m.Child[] = [
        <Link onclick={() => TasksWidget.getTaskModal(TasksWidget.getTaskTypes().get(task.type)!,
                                                      AbstractTask.fromJSON(task.toJSON()),
                                                      (updated: Task) => {
                                                        tasks()[index] = updated;
                                                      },
                                                      true,
                                                      pluginInfos,
                                                      autoSuggestions)!.render()}>
          <b>{task.description(pluginInfos)}</b>
        </Link>,
        <i>{task.attributes().runIf().join(", ")}</i>,
        <KeyValuePair inline={true} data={task.attributes().properties()}/>,
        task.attributes().onCancel()?.type || "No"
      ];

      if (isEditable) {
        cells.push(<Delete iconOnly={true}
                           onclick={() => tasks().splice(index, 1)}/>);
      }
      return cells;
    });
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
          ajaxOperationMonitor: Stream<OperationState>): m.Children {
    if (!this.autoSuggestions()) {
      this.fetchUpstreamPipelines(pipelineConfig.name(), routeParams.stage_name!);
    }

    return super.content(pipelineConfig, templateConfig, routeParams, ajaxOperationMonitor);
  }

  shouldShowSaveAndResetButtons(): boolean {
    return false;
  }

  protected renderer(entity: Job, templateConfig: TemplateConfig): m.Children {
    return <TasksWidget pluginInfos={this.pluginInfos}
                        autoSuggestions={this.autoSuggestions}
                        tasks={entity.tasks}
                        isEditable={true}/>;
  }

  protected selectedEntity(pipelineConfig: PipelineConfig, routeParams: PipelineConfigRouteParams): Job {
    return pipelineConfig.stages().findByName(routeParams.stage_name!)!.jobs().findByName(routeParams.job_name!)!;
  }

  private fetchPluginInfos() {
    return PluginInfoCRUD.all({type: ExtensionTypeString.TASK})
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
