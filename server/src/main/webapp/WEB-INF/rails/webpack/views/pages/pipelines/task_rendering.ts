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

// utils
import classnames from "classnames";
import {makeEvent} from "helpers/compat";
import {asSelector} from "helpers/css_proxies";
import {el, empty, removeEl, replaceWith} from "helpers/dom";
import {NonThrashingScheduler, Scheduler} from "helpers/scheduler";
import _ from "lodash";

// models
import {ExecTask, Task} from "models/pipeline_configs/task";
import {parseCLI, ParsedCommand} from "./task_parser";

// styles
import css from "./task_terminal.scss";

const sel = asSelector<typeof css>(css);

/** handles the element rendering and model write-back */
export class TerminalCrud {
  term: HTMLElement;
  model: (tasks?: Task[]) => Task[] | undefined;

  private scheduler: Scheduler = new NonThrashingScheduler(); // prevent premature and duplicative DOM updates; not critical, but good to have

  constructor(term: HTMLElement, model: (tasks?: Task[]) => Task[] | undefined) {
    this.term = term;
    this.model = model;
  }

  /** high-level API to open a previously saved Task for editing and update the DOM to reflect the end result */
  editCommand(taskLine: HTMLElement, editor: HTMLElement) {
    clearErrorsFromLine(taskLine);
    this.saveCommand(editor, false);

    editor.textContent = taskLine.textContent; // using `textContent` preserves newlines
    replaceWith(taskLine, editor).focus();
  }

  /** high-level API to save the current contents of the editor as a Task and update the DOM to reflect the end result */
  saveCommand(editor: HTMLElement, moveToBottom: boolean) {
    const line = editor.innerText.trim(); // preserves newlines in FF vs textContent

    if ("" !== line) {
      const parsed = parseCLI(line);
      editor.parentElement!.insertBefore(this.toTaskLine(parsed), empty(editor));
    }

    if (moveToBottom && editor !== editor.parentElement!.lastChild) {
      editor.parentElement!.appendChild(editor);
    }

    this.writeTasksToModel($$(sel.task, this.term));
  }

  /** computes a Task data model by inspecting a DOM element representing a task entry */
  toTaskModel(taskLine: HTMLElement) {
    const cmd = JSON.parse($(`[data-cmd]`, taskLine).getAttribute("data-cmd")!);
    const args = JSON.parse($(`[data-args]`, taskLine).getAttribute("data-args")!);

    const mod = $(`[data-cwd]`, taskLine);
    const workingDirectory = mod ? JSON.parse(mod.getAttribute("data-cwd")!) : void 0;

    return attachErrorAutoUpdates(new ExecTask(cmd, args, undefined, workingDirectory, [], undefined), taskLine, this.scheduler);
  }

  /** builds a DOM element representing a saved task entry from a shell-parsed result (i.e., from parsing the editor textContent) */
  toTaskLine(parsed: ParsedCommand) {
    const attrs: any = { class: classnames(css.task, { [css.hasErrors]: parsed.errors.hasErrors() }) };

    const children = [
      el("span", {"class": css.cmd, "data-cmd": JSON.stringify(parsed.cmd || "")}, parsed.rawCmd),
      el("span", {"class": css.args, "data-args": JSON.stringify(parsed.args || [])}, parsed.rawArgs)
    ];

    if (parsed.rawWd) {
      children.unshift(el("span", {"class": css.mod, "data-cwd": JSON.stringify(parsed.cwd)}, parsed.rawWd));
    }

    const errors = parsed.errors.hasErrors() ? [renderErrors(parsed.errors.allErrorsForDisplay())] : [];

    return el("pre", attrs, interlaceWithSpaces(children).concat(errors));
  }

  /** parses the DOM to extract all tasks and write back to the model */
  private writeTasksToModel(tasks: NodeListOf<HTMLElement>) {
    this.model(_.map(tasks, (t) => this.toTaskModel(t)));
    this.term.dispatchEvent(makeEvent("change", true, true));
  }
}

/** Utility/helper functions for DOM manipulation and fragment rendering */

function $(selector: string, context: HTMLElement): HTMLElement {
  return context.querySelector<HTMLElement>(selector) as HTMLElement;
}

function $$(selector: string, context: HTMLElement): NodeListOf<HTMLElement> {
  return context.querySelectorAll<HTMLElement>(selector);
}

function attachErrorAutoUpdates(model: Task, taskLine: HTMLElement, scheduler: Scheduler): Task {
  function updateErrorsOnElement() {
    if (taskLine.classList.contains(css.task)) {
      clearErrorsFromLine(taskLine);

      if (model.hasErrors()) {
        taskLine.appendChild(renderErrors(allTaskErrors(model)));
        taskLine.classList.add(css.hasErrors);
      } else {
        taskLine.classList.remove(css.hasErrors);
      }
    }
  }

  // auto-update the individual DOM element when altering the Errors object
  model.errors().on("error:change", () => scheduler.schedule(updateErrorsOnElement));
  model.attributes().errors().on("error:change", () => scheduler.schedule(updateErrorsOnElement));
  return model;
}

function renderErrors(errs: string[]) {
  return el("div", {class: css.errors},
    el("ul", null,
      _.map(errs, (msg) => el("li", null, msg))
    )
  );
}

function clearErrorsFromLine(taskLine: HTMLElement) {
  taskLine.querySelectorAll<HTMLElement>(sel.errors).forEach(removeEl);
  taskLine.classList.remove(css.hasErrors);
}

function allTaskErrors(task: Task) {
  return _.reduce([task, task.attributes()],
    (memo, model) => memo.concat(model.errors().allErrorsForDisplay()),
    [] as string[]);
}

// adds a single space character between each Node so long as the Node's textContent is a non-empty string
function interlaceWithSpaces(nodes: HTMLElement[]): Array<HTMLElement|string> {
  let len: number;

  return len = nodes.length, _.reduce(nodes, (memo, node, idx) => {
    memo.push(node);
    if ((len - 1) !== idx) {
      if (!!nodes[idx + 1].textContent) { // only interlace space if the next node is not empty
        memo.push(" ");
      }
    }
    return memo;
  }, [] as Array<HTMLElement|string>);
}
