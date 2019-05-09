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
    hitEnter(editText("foo -bar"));
    hitEnter(editText("whoami"));
    hitEnter(editText(`bash -c "echo 'Hello, McFly ~ is anybody home?'"`));
    assertModel(
      new ExecTask("foo", ["-bar"]),
      new ExecTask("whoami", []),
      new ExecTask("bash", ["-c", `echo 'Hello, McFly ~ is anybody home?'`])
    );
  });

  it("allows multiline entry without trailing backslashes", () => {
    hitEnter(editText(`find .
      -name package.json
      -type f`));
    assertModel(new ExecTask("find", [".", "-name", "package.json", "-type", "f"]));
  });

  it("multiline entries ignore trailing backslashes at newlines", () => {
    hitEnter(editText(`find . \\
      -name package.json \\
      -type f`));
    assertModel(new ExecTask("find", [".", "-name", "package.json", "-type", "f"]));
  });

  it("replaces task on edit", () => {
    // add some tasks
    hitEnter(editText("find ."));
    hitEnter(editText("whoami"));
    hitEnter(editText("echo hello"));

    expect(helper.q(sel.currentEditor).textContent).toBe("");
    expect(getTasksText()).toEqual(["find .", "whoami", "echo hello"]);
    assertModel(
      new ExecTask("find", ["."]),
      new ExecTask("whoami", []),
      new ExecTask("echo", ["hello"])
    );

    // replace the middle task via edit
    events.simulate(getTasks()[1], "click");
    hitEnter(editText("ls -la"));

    expect(helper.q(sel.currentEditor).textContent).toBe("");
    expect(getTasksText()).toEqual(["find .", "ls -la", "echo hello"]);
    assertModel(
      new ExecTask("find", ["."]),
      new ExecTask("ls", ["-la"]),
      new ExecTask("echo", ["hello"])
    );
  });

  it("doesn't create task for blank string", () => {
    hitEnter(editText(" \t\n\n\n "));
    assertModel();
  });

  function editText(text: string): Element {
    const el = helper.q(sel.currentEditor);
    el.textContent = text;
    return el;
  }

  function hitEnter(input: Element) {
    events.simulate(input, "keydown", {which: 13});
  }

  function getTasksText(): string[] {
    return _.map(getTasks()).map((t) => t.textContent!);
  }

  function getTasks() {
    return helper.qa(sel.task);
  }

  function plainObj(obj: any): any {
    return JSON.parse(JSON.stringify(obj));
  }

  function assertModel(...expected: Task[]) {
    return expect(plainObj(tasks())).toEqual(plainObj(expected));
  }
});
