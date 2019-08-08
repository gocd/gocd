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

import {bind} from "classnames/bind";
import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ButtonGroup} from "views/components/buttons";
import * as Buttons from "views/components/buttons/index";
import {ModalManager} from "views/components/modal/modal_manager";
import * as styles from "./index.scss";

const classnames = bind(styles);

interface State {
  selectedStep: Stream<string>;
}

export class Wizard extends MithrilComponent<any, State> {
  public id: string                      = "" + Math.random();
  private steps: Map<string, m.Children> = new Map<string, m.Children>();

  oninit(vnode: m.Vnode<any, State>) {
    vnode.state.selectedStep = stream(Array.from(this.steps.keys())[0]);
  }

  view(vnode: m.Vnode<any, State>): m.Vnode<any, any> {
    return (<div class={styles.wizard}>
      <header class={styles.wizardHeader}>
        {Array.from(this.steps.keys()).map((key) => {
          return <span class={classnames(styles.stepHeader, {[styles.selected]: vnode.state.selectedStep() === key})}
                       onclick={() => vnode.state.selectedStep(key)}>{key}</span>;
        })}
      </header>
      <div class={styles.wizardBody}>
        <div class={styles.stepBody}>{this.steps.get(vnode.state.selectedStep())}</div>
      </div>
      <footer class={styles.wizardFooter}>
        <ButtonGroup>
          <Buttons.Cancel onclick={this.close.bind(this)}>Cancel</Buttons.Cancel>
        </ButtonGroup>
      </footer>
    </div>);
  }

  render() {
    ModalManager.render(this);
  }

  close() {
    ModalManager.close(this);
  }

  public addStep(name: string, content: m.Children): Wizard {
    this.steps.set(name, content);
    return this;
  }
}
