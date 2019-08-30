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

import {getClipboardAsPlaintext, insertTextFromClipboard} from "helpers/compat";
import {asSelector} from "helpers/css_proxies";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {ExecTask} from "models/new_pipeline_configs/task";
import css from "./task_terminal.scss";

const sel = asSelector<typeof css>(css);

interface State {
  inputValue: Stream<string>;
  showCaveats: Stream<boolean>;
  toggleCaveats: () => void;
}

interface Attrs {
  task: ExecTask;
}

export class TaskEditor extends MithrilComponent<Attrs, State> {
  model?: ExecTask;
  terminal?: HTMLElement;

  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.inputValue  = Stream(vnode.attrs.task.command() ? vnode.attrs.task.command()! : "");
    vnode.state.showCaveats = Stream();

    vnode.state.toggleCaveats = () => {
      vnode.state.showCaveats(!vnode.state.showCaveats());
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    return <code class={css.execEditor}>
      <div data-test-id="caveats-container" onclick={vnode.state.toggleCaveats}
           class={`${css.caveats} ${vnode.state.showCaveats() ? css.open : undefined}`}>
        <span data-test-id="caveats-toggle-btn">Caveats</span>
        <p>This is not a real shell:</p>
        <p>- Pipes, loops, and conditionals will NOT work</p>
        <p>- Commands are not stateful; e.g., `cd foo` will NOT change cwd for subsequent commands</p>
      </div>
      <p data-test-id="caveats-instructions" class={css.comment}>
        # Press <strong>&lt;enter&gt;</strong> to save, <strong>&lt;shift-enter&gt;</strong> for newline
      </p>
      <pre contenteditable={true} class={css.currentEditor}>{m.trust(vnode.state.inputValue())}</pre>
    </code>;
  }

  oncreate(vnode: m.VnodeDOM<Attrs, State>) {
    this.terminal = vnode.dom as HTMLElement;
    this.model    = vnode.attrs.task;
    this.initTaskTerminal(vnode);
  }

  initTaskTerminal(vnode: m.VnodeDOM<Attrs, State>) {
    const ENTER  = 13;
    const EDITOR = this.$(sel.currentEditor);
    const self   = this;
    const term   = this.terminal!;

    term.addEventListener("input", (e: any) => {
      vnode.state.inputValue(e.target.innerText);
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

    // intercept paste and remove formatting
    term.addEventListener("paste", (e) => {
      e.preventDefault();

      insertTextFromClipboard(getClipboardAsPlaintext(e));
    });
  }

  saveCommand(el: HTMLElement) {
    const line = el.innerText.trim();
    this.model!.command(line);
    m.redraw();
  }

  private $(selector: string, context?: HTMLElement): HTMLElement {
    return (context || this.terminal!).querySelector<HTMLElement>(selector) as HTMLElement;
  }
}
