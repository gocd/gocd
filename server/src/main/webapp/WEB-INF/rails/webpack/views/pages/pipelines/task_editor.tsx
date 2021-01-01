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

import classnames from "classnames";
import {closest, getClipboardAsPlaintext, getSelection, insertTextFromClipboard, matches} from "helpers/compat";
import {asSelector} from "helpers/css_proxies";
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {Task} from "models/pipeline_configs/task";
import {BaseAttrs, FormField, RequiredFieldAttr} from "views/components/forms/input_fields";
import {TerminalCrud} from "./task_rendering";
import css from "./task_terminal.scss";

const sel = asSelector<typeof css>(css);

interface Attrs {
  tasks: (newValue?: Task[]) => Task[] | undefined;
}

export class TaskTerminalField extends FormField<Task[], RequiredFieldAttr> {
  renderInputField(vnode: m.Vnode<BaseAttrs<Task[]>>) {
    return <TaskEditor tasks={vnode.attrs.property}/>;
  }
}

export class TaskEditor extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs, {}>) {
    return <code class={css.execEditor}>

      <dl class={classnames(css.caveats, css.helpContent)}>
        <dt class={css.helpHeader}>Caveats</dt>
        <dd class={css.helpBody}>
          <p>This is not a real shell:</p>
          <p>- Pipes, loops, and conditionals will NOT work</p>
          <p>- Commands are not stateful; e.g., `cd foo` will NOT change cwd for subsequent commands (use the <code>CWD:</code> modifier instead)</p>
        </dd>
      </dl>

      <dl class={css.helpContent}>
        <dt class={css.helpHeader}>Help</dt>
        <dd class={css.helpBody}>
          <strong>Special Modifiers</strong>

          <ul>
            <li>
              <code>CWD:"path/to/other/dir"</code>: Sets the working directory to a sub-directory of the agent sandbox (i.e., the directory where pipelines run)
              <ul>
                <li>This modifier must specified before the command and its arguments, e.g., <code>CWD:"my/path" ls -A</code></li>
                <li>The specified path must be relative, but cannot traverse upward beyond the sandboxed directory</li>
              </ul>
            </li>
          </ul>
        </dd>
      </dl>

      <p class={css.comment}># Press <strong>&lt;enter&gt;</strong> to save, <strong>&lt;shift-enter&gt;</strong> for newline</p>
      <pre contenteditable={true} class={css.currentEditor}></pre>
    </code>;
  }

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    this.initTaskTerminal(vnode.dom as HTMLElement, vnode.attrs.tasks);
  }

  initTaskTerminal(container: HTMLElement, model: (newValue?: Task[]) => Task[] | undefined) {
    const term = new TerminalCrud(container, model);

    this.initCurrentEditor(container, term);
    this.initTaskLines(container, term);
    this.initAdjunctBehaviors(container);
  }

  initCurrentEditor(container: HTMLElement, crud: TerminalCrud) {
    const ENTER = 13;
    const EDITOR = currentEditor(container);

    EDITOR.addEventListener("blur", (e) => {
      crud.saveCommand(e.target as HTMLElement, true);
    });

    EDITOR.addEventListener("keydown", (e) => {
      if (ENTER === e.which && !e.shiftKey) {
        e.preventDefault();
        e.stopPropagation();
        crud.saveCommand(e.target as HTMLElement, false);
      }
    });
  }

  initTaskLines(container: HTMLElement, crud: TerminalCrud) {
    container.addEventListener("click", (e) => {
      if (e.which !== 1) { return; } // we only care about the primary click button (i.e., "left-click")

      if (isCurrentEditor(e.target)) {
        e.stopPropagation(); // prevents need to click twice to focus caret in contenteditable on page load
        return;
      }

      if (isTaskLine(e.target)) {
        e.stopPropagation();

        crud.editCommand(taskLine(e.target), currentEditor(container));
        return;
      }

      const selection = getSelection();

      // if the user has selected text within the terminal (e.g., to copy from `help`), don't
      // hijack the cursor by calling focus(); only do so when it's plain click
      if (!selection || "range" !== selection.type.toLowerCase()) {
        currentEditor(container).focus();
      }
    });
  }

  initAdjunctBehaviors(container: HTMLElement) {
    container.querySelectorAll(sel.helpHeader).forEach((help) => {
      help.addEventListener("click", (e) => {
        (e.currentTarget as HTMLElement).parentElement!.classList.toggle(css.open);
      });
    });

    // intercept paste and remove formatting
    container.addEventListener("paste", (e) => {
      e.preventDefault();

      insertTextFromClipboard(
        getClipboardAsPlaintext(e)
      );
    });
  }
}

function currentEditor(container: HTMLElement) {
  return container.querySelector(sel.currentEditor) as HTMLElement;
}

function isCurrentEditor(target: EventTarget | null) {
  return !!target && (target as HTMLElement).classList.contains(css.currentEditor);
}

function isTaskLine(target: EventTarget | null): target is HTMLElement {
  return !!target && matches(target as Element, `${sel.task},${sel.task} *`);
}

function taskLine(target: Element) {
  return closest(target, sel.task) as HTMLElement;
}
