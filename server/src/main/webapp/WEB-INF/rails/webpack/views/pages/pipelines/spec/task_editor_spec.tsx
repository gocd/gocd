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

import {asSelector} from "helpers/css_proxies";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {ExecTask, Task} from "models/pipeline_configs/task";
import * as events from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {TaskTerminalField} from "../task_editor";
import css from "../task_terminal.scss";

describe("AddPipeline: TaskTerminalField", () => {
  const helper = new TestHelper();
  const sel = asSelector<typeof css>(css);
  let tasks: Stream<Task[]>;

  beforeEach(() => {
    tasks = Stream();
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

  it("renders errors when commands cannot be parsed", () => {
    hitEnter(editText(`echo "hi`));
    expect(helper.q(sel.hasErrors)).toBeInDOM();
    expect(helper.text(sel.errors)).toBe("Unmatched quote.");
  });

  it("renders errors when modifiers fail validation", () => {
    hitEnter(editText(`CWD:/ ls`));
    expect(helper.q(sel.hasErrors)).toBeInDOM();
    expect(helper.text(sel.errors)).toBe("The specified CWD path must be relative, but cannot traverse upward beyond the sandboxed directory.");
  });

  it("renders error when only modifiers are provided without a command", () => {
    hitEnter(editText(`CWD:foo`)); // no command, only a modifier
    expect(helper.q(sel.hasErrors)).toBeInDOM();
    expect(helper.text(sel.errors)).toBe("Please provide a command to run.");
  });

  it("sets an error when multiline entries use trailing backslashes between lines ", () => {
    hitEnter(editText(`find . \\
      -name package.json \\
      -type f`));
    expect(helper.q(sel.hasErrors)).toBeInDOM();
    expect(helper.text(sel.errors)).toBe("Trailing backslashes in multiline commands will likely not work in GoCD tasks.");
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
    makeEditable(1);
    hitEnter(editText("ls -la"));
    expect(helper.q(sel.currentEditor).textContent).toBe("");
    expect(getTasksText()).toEqual(["find .", "ls -la", "echo hello"]);
    assertModel(
      new ExecTask("find", ["."]),
      new ExecTask("ls", ["-la"]),
      new ExecTask("echo", ["hello"])
    );
  });

  it("renders errors set from the model", (done) => {
    hitEnter(editText("whoami"));
    expect(helper.q(sel.task).textContent).toBe("whoami");
    expect(helper.q(sel.hasErrors)).toBeFalsy();

    const task = tasks()[0];
    task.errors().add("command", "no thanks");
    window.requestAnimationFrame(() => {
      expect(helper.q(sel.hasErrors)).toBeInDOM();
      expect(helper.text(sel.errors)).toBe("no thanks.");
      done();
    });
  });

  it("doesn't create task for blank string", () => {
    hitEnter(editText(" \t\n\n\n "));
    assertModel();
  });

  function makeEditable(index: number) {
    events.simulate(getTasks()[index], "click");
  }

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
