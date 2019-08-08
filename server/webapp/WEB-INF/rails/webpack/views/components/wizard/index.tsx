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
import * as Buttons from "views/components/buttons/index";
import {ModalManager} from "views/components/modal/modal_manager";
import * as styles from "./index.scss";

const classnames = bind(styles);
const uuid4      = require("uuid/v4");

export interface WizardState {
  selectedStep: Stream<m.Children | any>;
}

export abstract class Step {
  abstract header(): m.Children;

  abstract body(vnode: m.Vnode<any, WizardState>): m.Children;

  footer(wizard: Wizard, vnode: m.Vnode<any, WizardState>): m.Children {
    return [
      <Buttons.Cancel onclick={wizard.close.bind(wizard)}>Cancel</Buttons.Cancel>,
      <Buttons.Primary dataTestId="previous" onclick={wizard.previous.bind(wizard, vnode)}
                       disabled={wizard.isFirstStep(vnode)}>Previous</Buttons.Primary>,
      <Buttons.Primary dataTestId="next" onclick={wizard.next.bind(wizard, vnode)}
                       disabled={wizard.isLastStep(vnode)}>Next</Buttons.Primary>
    ];
  }
}

export class Wizard extends MithrilComponent<any, WizardState> {
  public id: string                                   = `modal-${uuid4()}`;
  private stepNameToContentMap: Map<m.Children, Step> = new Map<m.Children, Step>();
  private selectedStepIndex: Stream<number>           = stream(0);

  oninit(vnode: m.Vnode<any, WizardState>) {
    vnode.state.selectedStep = stream(Array.from(this.stepNameToContentMap.keys())[this.selectedStepIndex()]);
  }

  view(vnode: m.Vnode<any, WizardState>): m.Vnode<any, any> {
    const selectedStep = this.stepNameToContentMap.get(vnode.state.selectedStep());
    const footer       = selectedStep!.footer(this, vnode);
    return (<div class={styles.wizard}>
      <header class={styles.wizardHeader}>
        {Array.from(this.stepNameToContentMap.keys()).map((key) => {
          return <span class={classnames(styles.stepHeader, {[styles.selected]: vnode.state.selectedStep() === key})}
                       onclick={() => vnode.state.selectedStep(key)}>{key}</span>;
        })}
      </header>
      <div class={styles.wizardBody}>
        <div class={styles.stepBody}>{selectedStep!.body(vnode)}</div>
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

  previous(vnode: m.Vnode<any, WizardState>) {
    const steps        = Array.from(this.stepNameToContentMap.keys());
    const currentIndex = steps.indexOf(vnode.state.selectedStep());
    vnode.state.selectedStep(steps[currentIndex - 1]);
  }

  next(vnode: m.Vnode<any, WizardState>) {
    const steps        = Array.from(this.stepNameToContentMap.keys());
    const currentIndex = steps.indexOf(vnode.state.selectedStep());
    vnode.state.selectedStep(steps[currentIndex + 1]);
  }

  isFirstStep(vnode: m.Vnode<any, WizardState>) {
    return Array.from(this.stepNameToContentMap.keys())
                .indexOf(vnode.state.selectedStep()) === 0;
  }

  isLastStep(vnode: m.Vnode<any, WizardState>) {
    const steps = Array.from(this.stepNameToContentMap.keys());
    return steps.indexOf(vnode.state.selectedStep()) === steps.length - 1;
  }

  defaultStepIndex(index: number) {
    if (index > 0) {
      this.selectedStepIndex(index - 1);
    }
    m.redraw();
    return this;
  }

  public addStep(step: Step): Wizard {
    this.stepNameToContentMap.set(step.header(), step);
    return this;
  }
}
