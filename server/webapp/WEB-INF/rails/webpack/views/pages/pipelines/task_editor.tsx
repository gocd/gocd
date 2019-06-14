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
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {ExecTask, Task} from "models/pipeline_configs/task";
import Shellwords from "shellwords-ts";
import {BaseAttrs, FormField, RequiredFieldAttr} from "views/components/forms/input_fields";
import * as css from "./components.scss";

const sel = asSelector<typeof css>(css);

interface Attrs {
  tasks: (newValue?: Task[]) => Task[];
}

interface ParsedCommand {
  cmd: string;
  args: string[];
  rawCmd: string;
  rawArgs: string;
}

export class TaskTerminalField extends FormField<Task[], RequiredFieldAttr> {
  renderInputField(vnode: m.Vnode<BaseAttrs<Task[]>>) {
    return <TaskEditor tasks={vnode.attrs.property}/>;
  }
}

export class TaskEditor extends MithrilViewComponent<Attrs> {
  model?: (newValue?: Task[]) => Task[];
  terminal?: HTMLElement;

  view(vnode: m.Vnode<Attrs, {}>) {
    return <code class={css.execEditor}>
      <div class={css.caveats}>
        <span>Caveats</span>
        <p>This is not a real shell:</p>
        <p>- Pipes, loops, and conditionals will NOT work</p>
        <p>- Commands are not stateful; e.g., `cd foo` will NOT change cwd for subsequent commands</p>
      </div>
      <p class={css.comment}># Press <strong>&lt;enter&gt;</strong> to save, <strong>&lt;shift-enter&gt;</strong> for newline</p>
      <pre contenteditable={true} class={css.currentEditor}></pre>
    </code>;
  }

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    this.terminal = vnode.dom as HTMLElement;
    this.model = vnode.attrs.tasks;
    this.initTaskTerminal();
  }

  initTaskTerminal() {
    const ENTER  = 13;
    const EDITOR = this.$(sel.currentEditor);
    const self   = this;
    const term   = this.terminal!;

    EDITOR.addEventListener("blur", (e: Event) => {
      self.saveCommand(EDITOR, true);

      // allow click through for button actions
      if (isMouseEvent(e) && isHtmlElement(e.relatedTarget) && e.relatedTarget.closest(sel.button)) {
        e.relatedTarget.click();
      }
    });

    this.$(`${sel.caveats} span`).addEventListener("click", (e) => {
      (e.currentTarget as HTMLElement).parentElement!.classList.toggle(css.open);
    });

    term.addEventListener("keydown", (e) => {
      if (EDITOR === e.target) {
        if (ENTER === e.which && !e.shiftKey) {
          e.preventDefault();
          e.stopPropagation();
          self.saveCommand(e.target as HTMLElement);
        }
      }
    });

    term.addEventListener("click", (e) => {
      if (EDITOR === e.target) {
        e.stopPropagation(); // prevents need to click twice to focus caret in contenteditable on page load
      }

      const el = e.target as HTMLElement;

      if (el.matches(`${sel.task},${sel.task} *`)) {
        e.stopPropagation();

        self.editCommand(el.closest(sel.task) as HTMLElement, EDITOR);
      } else {
        EDITOR.focus();
      }
    });
  }

  editCommand(el: HTMLElement, editEl: HTMLElement) {
    this.saveCommand(editEl);

    editEl.textContent = TaskEditor.readTaskText(el);
    replaceElement(el, editEl).focus();
  }

  saveCommand(el: HTMLElement, moveToBottom?: boolean) {
    const line = el.innerText.trim(); // preserves newlines in FF vs textContent

    if ("" !== line) {
      const parsed = TaskEditor.parseCLI(line);
      el.parentNode!.insertBefore(TaskEditor.toTaskEl(parsed), empty(el));
    }

    if (moveToBottom && el !== el.parentNode!.lastChild) {
      el.parentNode!.appendChild(el);
    }

    this.writeTasksToModel(this.$$(sel.task));
  }

  private static toTaskEl(parsed: ParsedCommand): HTMLElement {
    return newEl("pre", {class: css.task}, [
      newEl("span", {"class": css.cmd, "data-cmd": JSON.stringify(parsed.cmd)}, parsed.rawCmd),
      "" === parsed.rawArgs ? "" : " ",
      newEl("span", {"class": css.args, "data-args": JSON.stringify(parsed.args)}, parsed.rawArgs)
    ]);
  }

  private static parseCLI(raw: string): ParsedCommand {
    let rawCmd = raw, rawArgs = ""; // rawCmd/Args preserve the exact formatting of the user's input
    let extractedCommand = false; // flag to ensure we only set these once
    const args = _.filter(Shellwords.split(raw, (token: string) => {
      if (!extractedCommand) {
        // get the raw cmd string and argStr to
        // preserve exactly what the user typed, so as
        // to include whitespace and escape chars
        rawCmd = raw.slice(0, token.length).trim();
        rawArgs = raw.slice(token.length).trim();
        extractedCommand = true;
      }
    }), (s) => "\\" !== s.trim());

    const cmd = args.shift()!;

    return { cmd, args, rawCmd, rawArgs };
  }

  private static readTaskText(t: Element): string {
    // using `textContent` preserves newlines
    return `${t.querySelector(sel.cmd)!.textContent!.trim()} ${t.querySelector(sel.args)!.textContent!.trim()}`;
  }

  private static toTask(t: Element): Task {
    const cmd = JSON.parse(t.querySelector(`[data-cmd]`)!.getAttribute("data-cmd")!);
    const args = JSON.parse(t.querySelector(`[data-args]`)!.getAttribute("data-args")!);
    return new ExecTask(cmd, args);
  }

  private $(selector: string, context?: HTMLElement): HTMLElement {
    return (context || this.terminal!).querySelector<HTMLElement>(selector) as HTMLElement;
  }

  private $$(selector: string, context?: HTMLElement): NodeListOf<HTMLElement> {
    return (context || this.terminal!).querySelectorAll<HTMLElement>(selector);
  }

  private writeTasksToModel(tasks: NodeListOf<Element>) {
    this.model!(_.map(tasks, TaskEditor.toTask));
  }
}

type Child = string | Node;
function newEl(tag: string, options: any, children: Child | Child[]): HTMLElement {
  const el = document.createElement(tag);

  for (const key of Object.keys(options)) {
    el.setAttribute(key, options[key]);
  }

  if (children instanceof Array) {
    for (const child of children) {
      appendTo(el, asNode(child));
    }
  } else {
    appendTo(el, asNode(children));
  }
  return el;
}

function appendTo(el: Node, child?: Node) {
  if (child) { el.appendChild(child); }
}

function asNode(subj: Child): Node | undefined {
  if ("string" === typeof subj) {
    if ("" === subj) { return undefined; }
    return document.createTextNode(subj);
  }

  if (subj instanceof Node) {
    return subj;
  }

  throw new TypeError(`Expected ${subj} to be either a string or Node`);
}

function replaceElement(src: HTMLElement, dst: HTMLElement): HTMLElement {
  if (src.parentElement) {
    src.parentElement.insertBefore(dst, src);
    src.parentElement.removeChild(src);
  } else {
    src.remove();
  }
  return dst;
}

function empty(el: HTMLElement): HTMLElement {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}

function isHtmlElement(el: any): el is HTMLElement {
  return !!(el as HTMLElement).classList;
}

function isMouseEvent(e: Event): e is MouseEvent {
  return !!(e as MouseEvent).relatedTarget;
}
