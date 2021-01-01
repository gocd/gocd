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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import * as Buttons from "views/components/buttons/index";
import {ModalManager} from "views/components/modal/modal_manager";
import styles from "./index.scss";

import { v4 as uuid4 } from 'uuid';

const classnames = bind(styles);

export abstract class Step {
  abstract header(): m.Children;

  abstract body(): m.Children;

  footer(wizard: Wizard): m.Children {
    return [
      <Buttons.Cancel onclick={wizard.close.bind(wizard)}
                      data-test-id="cancel">Cancel</Buttons.Cancel>,
      <Buttons.Primary data-test-id="previous" onclick={wizard.previous.bind(wizard, 1)} align="right"
                       disabled={wizard.isFirstStep()}>Previous</Buttons.Primary>,
      <Buttons.Primary data-test-id="next" onclick={wizard.next.bind(wizard, 1)} align="right"
                       disabled={wizard.isLastStep()}>Next</Buttons.Primary>
    ];
  }
}

export class Wizard extends MithrilViewComponent {

  get allowHeaderClick() {
    return this._allowHeaderClick;
  }

  set allowHeaderClick(value: boolean) {
    this._allowHeaderClick = value;
  }
  public id: string                         = `modal-${uuid4()}`;
  private steps: Step[]                     = [];
  private selectedStepIndex: Stream<number> = Stream(0);
  private closeListener?: CloseListener;
  private _allowHeaderClick: boolean        = true;

  view(vnode: m.Vnode): m.Vnode {
    const selectedStep = this.steps[this.selectedStepIndex()];
    const footer       = selectedStep!.footer(this);
    return (<div class={styles.wizard}>
      <header class={styles.wizardHeader}>
        {this.steps.map((step, index) => {
          return <span
            class={classnames(styles.stepHeader,
                              {[styles.selected]: step.header() === selectedStep.header()},
                              {[styles.clickable]: this.allowHeaderClick})}
            onclick={this.headerClicked.bind(this, index)}>{step.header()}</span>;
        })}
      </header>
      <div class={styles.wizardBody} data-test-id="modal-body">
        <div class={styles.stepBody}>{selectedStep!.body()}</div>
      </div>
      <footer class={styles.wizardFooter}>
        {footer}
      </footer>
    </div>);
  }

  render() {
    ModalManager.render(this);
    return this;
  }

  close() {
    ModalManager.close(this);
    if (this.closeListener !== undefined) {
      this.closeListener.onClose();
    }
  }

  previous(skip = 1) {
    this.selectedStepIndex(this.selectedStepIndex() - skip);
    Wizard.scrollToTop();
  }

  next(skip = 1) {
    this.selectedStepIndex(this.selectedStepIndex() + skip);
    Wizard.scrollToTop();
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

  public setCloseListener(closeListener: CloseListener): Wizard {
    this.closeListener = closeListener;
    return this;
  }

  private static scrollToTop() {
    const wizardBody = document.getElementsByClassName(styles.wizardBody);
    if (wizardBody && wizardBody.length) {
      wizardBody[0].scrollTop = 0;
    }
  }

  private headerClicked(index: number) {
    if (this.allowHeaderClick) {
      this.selectedStepIndex(index);
    }
  }
}

export interface CloseListener {
  onClose(): void;
}
