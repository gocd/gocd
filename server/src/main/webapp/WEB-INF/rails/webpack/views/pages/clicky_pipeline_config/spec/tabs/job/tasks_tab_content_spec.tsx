/*
 * Copyright Thoughtworks, Inc.
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
import {JobTestData} from "models/pipeline_configs/spec/test_data";
import {ExecTask, ExecTaskAttributes, FetchArtifactTask, FetchTaskAttributes, Task} from "models/pipeline_configs/task";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {EntityReOrderHandler} from "views/pages/clicky_pipeline_config/tabs/common/re_order_entity_widget";
import {Attrs, State, TasksWidget} from "views/pages/clicky_pipeline_config/tabs/job/tasks_tab_content";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Tasks Tab Content", () => {
  const helper = new TestHelper();
  afterEach(helper.unmount.bind(helper));

  it("should render table header", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    mount(job);

    expect(helper.qa("th", helper.byTestId("table-header-row"))[1]).toContainText("Task Type");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[2]).toContainText("Properties");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[3]).toContainText("Run If Condition");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[4]).toContainText("On Cancel");
    expect(helper.qa("th", helper.byTestId("table-header-row"))[5]).toContainText("Remove");
  });

  it("should render delete task icon", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tasks().push(new ExecTask("ls", []));
    job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
    mount(job);

    expect(helper.byTestId("task-0-delete-icon")).toBeInDOM();
    expect(helper.byTestId("task-1-delete-icon")).toBeInDOM();
  });

  it("should disable delete task when there is only one task", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
    mount(job);

    expect(helper.byTestId("task-0-delete-icon")).toBeInDOM();
    const expectedMsg = "Can not delete the only task from the job.";
    expect(helper.byTestId("task-0-delete-icon").title).toEqual(expectedMsg);
    expect(helper.byTestId("task-0-delete-icon")).toBeDisabled();
  });

  it("should not disable delete task when there are more than one tasks", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tasks().push(new ExecTask("ls", []));
    job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
    mount(job);

    expect(helper.byTestId("task-0-delete-icon")).toBeInDOM();
    expect(helper.byTestId("task-0-delete-icon")).not.toBeDisabled();

    expect(helper.byTestId("task-1-delete-icon")).toBeInDOM();
    expect(helper.byTestId("task-1-delete-icon")).not.toBeDisabled();
  });

  it("should delete task on click of delete icon", () => {
    const job = Job.fromJSON(JobTestData.with("test"));
    job.tasks().push(new ExecTask("pwd", []));
    job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
    mount(job);

    expect(job.tasks()).toHaveLength(2);

    helper.clickByTestId("task-0-delete-icon");

    const body = document.body;
    expect(helper.byTestId("modal-title", body)).toContainText("Delete Task");
    expect(helper.byTestId("modal-body", body)).toContainText("Do you want to delete the task at index '1'?");

    helper.clickByTestId("primary-action-button", body);

    expect(job.tasks()).toHaveLength(1);
  });

  for (const adding of [true, false]) {
    it(`should bind only the modal task errors when ${adding ? "adding" : "editing"} and restore tasks on failure`, async () => {
      const task = FetchArtifactTask.from({
        artifact_origin: "external", pipeline: "upstream", stage: "build", job: "job",
        artifact_id: "artifact", configuration: [{key: "prefix", value: "invalid-prefix"}], run_if: []
      });
      task.attributes().onCancel(new ExecTask("cleanup", []));
      const original = [FetchArtifactTask.from(task.toJSON().attributes), FetchArtifactTask.from(task.toJSON().attributes)];
      const tasks = Stream<Task[]>(original.slice());
      const widget = new TasksWidget();
      const save = jasmine.createSpy();
      const modal = TasksWidget.getTaskModal("Fetch Artifact", task, () => Promise.resolve(), true,
                                             new PluginInfos(), save, () => undefined, false, Stream({}));
      const close = spyOn(modal, "close");
      const flashMessage = new FlashMessageModelWithTimeout();
      const reset = () => undefined;
      const vnode: m.Vnode<Attrs, State> = {tag: "div", attrs: {
        tasks, stageName: "selected-stage", jobName: "selected-job", pipelineConfigSave: save,
        flashMessage, isEditable: true, pluginInfos: Stream(new PluginInfos()), autoSuggestions: Stream({}),
        pipelineConfigReset: reset, entityReOrderHandler: new EntityReOrderHandler("Task", flashMessage, save, reset, () => false)
      }, state: {modal, allTaskTypes: [], selectedTaskTypeToAdd: Stream("Fetch Artifact")}};
      const index = adding ? original.length : 1;
      const taskResponse = {
        attributes: {
          errors: {artifact_id: ["Invalid artifact"], unknown: ["Unknown task field"]},
          configuration: [{errors: {configuration_value: ["Invalid prefix"]}}],
          on_cancel: {attributes: {errors: {command: ["Invalid cleanup"]}}}
        }
      };
      const siblingResponse = JSON.parse(JSON.stringify(taskResponse));
      siblingResponse.attributes.errors = {artifact_id: ["Sibling artifact error"]};
      siblingResponse.attributes.on_cancel.attributes.errors = {command: ["Sibling cleanup error"]};
      siblingResponse.attributes.configuration[0].errors.configuration_value = ["Sibling prefix error"];
      const responseTasks: any[] = [siblingResponse, siblingResponse];
      responseTasks[index] = taskResponse;
      const data = {
        errors: {name: ["Invalid configuration"]},
        stages: [{name: "other-stage", jobs: [{name: "selected-job", tasks: [siblingResponse]}]}, {
          name: "selected-stage", jobs: [{name: "other-job", tasks: [siblingResponse]}, {
            name: "selected-job", tasks: responseTasks
          }]
        }]
      };
      save.and.callFake(() => {
        // The enclosing tab consumes errors before rejecting the modal's save.
        task.consumeErrorsResponseForPipelineConfigSPA(taskResponse);
        return Promise.reject(JSON.stringify({body: JSON.stringify({message: "Save failed", data})}));
      });

      helper.mount(() => modal.renderFlashMessage());
      for (let attempt = 0; attempt < 2; attempt++) {
        await (adding ? widget.onTaskSave(vnode) : widget.onTaskUpdate(vnode, index, task));
        expect(tasks()).toEqual(original);
        expect(close).not.toHaveBeenCalled();
        expect(task.attributes().errors().errorsForDisplay("artifactId")).toBe("Invalid artifact.");
        expect(task.attributes().onCancel()!.attributes().errors().errorsForDisplay("command")).toBe("Invalid cleanup.");
        expect((task.attributes() as FetchTaskAttributes).configuration().get(0).errors).toEqual(["Invalid prefix"]);
        expect(original[0].attributes().errors().hasErrors()).toBe(false);
        helper.redraw();
        expect(helper.root).toContainText("Save failed");
        expect(helper.root).toContainText("Invalid configuration");
        expect(helper.root).toContainText("Unknown task field");
        expect(helper.root).toContainText("Sibling prefix error");
        expect(helper.root).not.toContainText("Invalid prefix");
        expect(helper.root).not.toContainText("Invalid artifact");
        expect(helper.root).not.toContainText("Invalid cleanup");
      }

      for (const missing of ["stage", "job", "task"]) {
        const missingData = JSON.parse(JSON.stringify(data));
        if (missing === "stage") {
          missingData.stages[1].name = "missing";
        } else if (missing === "job") {
          missingData.stages[1].jobs[1].name = "missing";
        } else {
          missingData.stages[1].jobs[1].tasks.splice(index, 1);
        }
        save.and.returnValue(Promise.reject(JSON.stringify({body: JSON.stringify({message: "Save failed", data: missingData})})));
        await (adding ? widget.onTaskSave(vnode) : widget.onTaskUpdate(vnode, index, task));
        helper.redraw();
        expect(helper.root).toContainText(missing === "task" ? "Sibling prefix error" : "Invalid prefix");
        expect(tasks()).toEqual(original);
      }

      save.and.returnValue(Promise.reject(JSON.stringify({message: "Save failed"})));
      await (adding ? widget.onTaskSave(vnode) : widget.onTaskUpdate(vnode, index, task));
      expect(modal.flashMessage.message).toBe("Save failed");
      expect(tasks()).toEqual(original);

      save.and.returnValue(Promise.resolve());
      await (adding ? widget.onTaskSave(vnode) : widget.onTaskUpdate(vnode, index, task));
      expect(close).toHaveBeenCalled();
    });
  }

  describe("Exec Task", () => {
    let job: Job;
    beforeEach(() => {
      job = Job.fromJSON(JobTestData.with("test"));
      job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));
      mount(job);
    });

    it("should render exec task type", () => {
      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[1]).toContainText("Custom Command");
    });

    it("should render failed run if condition", () => {
      job.tasks()[0].attributes().runIf(["failed"]);
      helper.redraw();

      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[3]).toContainText("failed");
    });

    it("should render passed run if condition when none specified", () => {
      job.tasks()[0].attributes().runIf([]);
      helper.redraw();

      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[3]).toContainText("passed");
    });

    it("should render properties", () => {
      expect(helper.byTestId("key-value-key-command")).toContainText("Command");
      expect(helper.byTestId("key-value-value-command")).toContainText("ls");
      expect(helper.byTestId("key-value-key-arguments")).toContainText("Arguments");
      expect(helper.byTestId("key-value-value-arguments")).toContainText("-a -l -h");
      expect(helper.byTestId("key-value-key-working-directory")).toContainText("Working Directory");
      expect(helper.byTestId("key-value-value-working-directory")).toContainText("(Not specified)");
    });

    it("should render working directory property when specified", () => {
      (job.tasks()[0].attributes() as ExecTaskAttributes).workingDirectory("/tmp");
      helper.redraw();

      expect(helper.byTestId("key-value-key-working-directory")).toContainText("Working Directory");
      expect(helper.byTestId("key-value-value-working-directory")).toContainText("/tmp");
    });

    it("should render no on cancel specified", () => {
      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[4]).toContainText("No");
    });

    it("should render on cancel task type", () => {
      (job.tasks()[0].attributes() as ExecTaskAttributes).onCancel(new ExecTask("ls", []));
      helper.redraw();

      const rows = helper.qa("td", helper.byTestId("table-row"));
      expect(rows[4]).toContainText("exec");
    });
  });

  describe("Read Only", () => {
    beforeEach(() => {
      const job = Job.fromJSON(JobTestData.with("test"));
      job.tasks().push(new ExecTask("ls", []));
      job.tasks().push(new ExecTask("ls", ["-a", "-l", "-h"]));

      mount(job, false);
    });

    it("should not render task delete icon", () => {
      expect(helper.byTestId("task-0-delete-icon")).not.toBeInDOM();
      expect(helper.byTestId("task-1-delete-icon")).not.toBeInDOM();
    });

    it("should not render add task", () => {
      expect(helper.byTestId("add-task-button")).not.toBeInDOM();
    });
  });

  function mount(job: Job, isEditable: boolean = true) {
    helper.mount(() => {
      const flashMessage         = new FlashMessageModelWithTimeout();
      const pipelineConfigSave   = jasmine.createSpy().and.returnValue(Promise.resolve());
      const pipelineConfigReset  = jasmine.createSpy().and.returnValue(Promise.resolve());
      const entityReOrderHandler = new EntityReOrderHandler("",
                                                            flashMessage,
                                                            pipelineConfigSave,
                                                            pipelineConfigReset,
                                                            () => false);

      return <TasksWidget stageName="stage" jobName="test" tasks={job.tasks}
                          isEditable={isEditable}
                          entityReOrderHandler={entityReOrderHandler}
                          flashMessage={flashMessage}
                          pipelineConfigSave={pipelineConfigSave}
                          pipelineConfigReset={pipelineConfigReset}
                          autoSuggestions={Stream({})}
                          pluginInfos={Stream(new PluginInfos())}/>;
    });
  }
});
