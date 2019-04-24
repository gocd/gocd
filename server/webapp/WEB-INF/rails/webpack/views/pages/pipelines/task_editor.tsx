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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import {ExecTask, Task} from "models/pipeline_configs/task";
import * as styles from "./components.scss";
const Shellwords = require('./shellwords').default;

interface Attrs {
  tasks: Stream<Task[]>;
}

export class TaskEditor extends MithrilViewComponent<Attrs> {
  model?: Stream<Task[]>;
  terminal?: HTMLElement;

  view(vnode: m.Vnode<Attrs, {}>) {
    return <code className={styles.execEditor}>
      <pre contenteditable={true} className={styles.currentEditor}></pre>
    </code>;
  }

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    this.terminal = vnode.dom as HTMLElement;
    this.model = vnode.attrs.tasks;
    this.initTaskTerminal();
  }

  initTaskTerminal() {
    const ENTER = 13;
    const EDITOR = this.terminal!.querySelector(c(styles.currentEditor)) as HTMLElement;
    const self = this;

    document.body.addEventListener("click", (e) => {
      self.saveCommand(EDITOR, true);
    });

  // document.getElementById("save-and-run").addEventListener("click", function(e) {
  //   this.saveCommand(EDITOR, true);
  // });

    this.terminal!.addEventListener("keydown", (e) => {
      if (EDITOR === e.target) {

        switch (e.which) {
          case ENTER:
            if (!e.shiftKey) {
              e.preventDefault();
              e.stopPropagation();
              self.saveCommand(e.target as HTMLElement);
            }
            break;
        }
      }
    });

    this.terminal!.addEventListener("click", (e) => {
      if (EDITOR === e.target) {
        e.stopPropagation(); // prevents need to click twice to focus caret in contenteditable on page load
      }

      if ((e.target as HTMLElement).matches(`.${styles.task},.${styles.task} *`)) {
        self.editCommand(e, EDITOR);
      }
    });
  }

  editCommand(e: Event, editEl: HTMLElement) {
    e.stopPropagation();
    const el = closest(e.target as HTMLElement, c(styles.task));
    this.saveCommand(editEl);
    editEl.textContent = [ // be sure to use textContent to preserve newlines
      el.querySelector(c(styles.cmd))!.textContent,
      el.querySelector(c(styles.args))!.textContent
    ].join(" ").trim();

    el.parentNode!.insertBefore(editEl, el);
    el.parentNode!.removeChild(el);
    editEl.focus();
  }

  saveCommand(el: HTMLElement, moveToBottom?: boolean) {
    const line = el.innerText.trim(); // preserves newlines in FF vs textContent

    if ("" !== line) {
      let cmdStr = line, argStr = "";
      let extractedCommand = false;
      const args = Shellwords.split(line, (token: string) => {
        if (!extractedCommand) {
          // get the raw cmd string and argStr to
          // preserve exactly what the user typed, so as
          // to include whitespace and escape chars
          cmdStr = line.slice(0, token.length).trim();
          argStr = line.slice(token.length).trim();
          extractedCommand = true;
        }
      });
      const cmd = args.shift();

      const saved = newEl("pre", {class: styles.task}, [
        newEl("span", {"class": styles.cmd, "data-cmd": JSON.stringify(cmd)}, cmdStr),
        newEl("span", {"class": styles.args, "data-args": JSON.stringify(args)}, argStr)
      ]);

      el.parentNode!.insertBefore(saved, empty(el));
    }

    if (moveToBottom && el !== el.parentNode!.lastChild) {
      el.parentNode!.appendChild(el);
    }

    this.writeToModel();
  }

  private writeToModel() {
    const tasks = this.terminal!.querySelectorAll(c(styles.task));
    this.model!(_.map(tasks, (task) => {
      const cmd = JSON.parse(task.querySelector(`[data-cmd]`)!.getAttribute("data-cmd")!);
      const args = JSON.parse(task.querySelector(`[data-args]`)!.getAttribute("data-args")!);
      return (new ExecTask(cmd, args) as Task);
    }));
  }
}

type Child = string | Node;
function newEl(tag: string, options: any, children: Child | Child[]): HTMLElement {
  const el = document.createElement(tag);

  Object.keys(options).forEach((key) => {
    el.setAttribute(key, options[key]);
  });

  if (children instanceof Array) {
    for (const child of children) {
      if ("string" === typeof child) {
        el.appendChild(document.createTextNode(child));
      } else {
        el.appendChild(child);
      }
    }
  } else if ("string" === typeof children) {
    el.appendChild(document.createTextNode(children as string));
  } else {
    el.appendChild(children);
  }
  return el;
}

function empty(el: HTMLElement): HTMLElement {
  while (el.firstChild) {
    el.removeChild(el.firstChild);
  }
  return el;
}

function closest(el: HTMLElement, selector: string): HTMLElement {
  while (!el.matches(selector)) {
    el = el.parentNode as HTMLElement;
  }
  return el;
}

function c(className: string) {
  return `.${className}`;
}
