/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import asSelector from "helpers/selector_proxy";
import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {ExecTask, Task} from "models/pipeline_configs/task";
import * as events from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import * as css from "../components.scss";
import {TaskTerminalField} from "../task_editor";

describe("AddPipeline: TaskTerminalField", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof css>(css);
  let tasks: Stream<Task[]>;

  beforeEach(() => {
    tasks = stream();
    helper.mount(() => {
      return <TaskTerminalField property={tasks}/>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("adds tasks", () => {
    hitEnter(helper.find(sel.currentEditor).text("foo -bar")[0]);
    hitEnter(helper.find(sel.currentEditor).text("whoami")[0]);
    hitEnter(helper.find(sel.currentEditor).text(`bash -c "echo 'Hello, McFly ~ is anybody home?'"`)[0]);
    assertModel(
      new ExecTask("foo", ["-bar"]),
      new ExecTask("whoami", []),
      new ExecTask("bash", ["-c", `echo 'Hello, McFly ~ is anybody home?'`])
    );
  });

  it("replaces task on edit", () => {
    // add some tasks
    hitEnter(helper.find(sel.currentEditor).text("find .")[0]);
    hitEnter(helper.find(sel.currentEditor).text("whoami")[0]);
    hitEnter(helper.find(sel.currentEditor).text("echo hello")[0]);

    expect(helper.find(sel.currentEditor).text()).toBe("");
    expect(getTasksText()).toEqual(["find .", "whoami", "echo hello"]);
    assertModel(
      new ExecTask("find", ["."]),
      new ExecTask("whoami", []),
      new ExecTask("echo", ["hello"])
    );

    // replace the middle task via edit
    events.simulate(getTasks()[1], "click");
    hitEnter(helper.find(sel.currentEditor).text("ls -la")[0]);

    expect(helper.find(sel.currentEditor).text()).toBe("");
    expect(getTasksText()).toEqual(["find .", "ls -la", "echo hello"]);
    assertModel(
      new ExecTask("find", ["."]),
      new ExecTask("ls", ["-la"]),
      new ExecTask("echo", ["hello"])
    );
  });

  it("doesn't create task for blank string", () => {
    hitEnter(helper.find(sel.currentEditor).text(" \t\n\n\n ")[0]);
    assertModel();
  });

  function hitEnter(input: HTMLElement) {
    events.simulate(input, "keydown", {which: 13});
  }

  function getTasksText(): string[] {
    return _.map(getTasks()).map((t) => t.textContent!);
  }

  function getTasks() {
    return helper.find(sel.task);
  }

  function plainObj(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  function assertModel(...expected: Task[]) {
    return expect(plainObj(tasks())).toEqual(plainObj(expected));
  }
});
