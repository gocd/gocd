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
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import * as Buttons from "views/components/buttons/index";
import {ModalManager} from "views/components/modal/modal_manager";
import * as styles from "./index.scss";

const classnames = bind(styles);
const uuid4      = require("uuid/v4");

export abstract class Step {
  abstract header(): m.Children;

  abstract body(): m.Children;

  footer(wizard: Wizard): m.Children {
    return [
      <Buttons.Cancel onclick={wizard.close.bind(wizard)}>Cancel</Buttons.Cancel>,
      <Buttons.Primary dataTestId="previous" onclick={wizard.previous.bind(wizard)}
                       disabled={wizard.isFirstStep()}>Previous</Buttons.Primary>,
      <Buttons.Primary dataTestId="next" onclick={wizard.next.bind(wizard)}
                       disabled={wizard.isLastStep()}>Next</Buttons.Primary>
    ];
  }
}

export class Wizard extends MithrilViewComponent<{}> {
  public id: string                         = `modal-${uuid4()}`;
  private steps: Step[]                     = [];
  private selectedStepIndex: Stream<number> = stream(0);

  view(vnode: m.Vnode<{}>): m.Vnode<{}> {
    const selectedStep = this.steps[this.selectedStepIndex()];
    const footer       = selectedStep!.footer(this);
    return (<div class={styles.wizard}>
      <header class={styles.wizardHeader}>
        {this.steps.map((step, index) => {
          return <span
            class={classnames(styles.stepHeader, {[styles.selected]: step.header() === selectedStep.header()})}
            onclick={() => this.selectedStepIndex(index)}>{step.header()}</span>;
        })}
      </header>
      <div class={styles.wizardBody}>
        <div class={styles.stepBody}>{selectedStep!.body()}</div>
      </div>
      <footer class={styles.wizardFooter}>
        {footer}
      </footer>
    </div>);
  }

  render() {
    ModalManager.render(this);
  }

  close() {
    ModalManager.close(this);
  }

  previous() {
    this.selectedStepIndex(this.selectedStepIndex() - 1);
  }

  next() {
    this.selectedStepIndex(this.selectedStepIndex() + 1);
  }

  isFirstStep() {
    return this.selectedStepIndex() === 0;
  }

  isLastStep() {
    return this.selectedStepIndex() === this.steps.length - 1;
  }

  defaultStepIndex(index: number) {
    if (index > 0) {
      this.selectedStepIndex(index - 1);
    }
    m.redraw();
    return this;
  }

  public addStep(step: Step): Wizard {
    this.steps.push(step);
    return this;
  }
}
