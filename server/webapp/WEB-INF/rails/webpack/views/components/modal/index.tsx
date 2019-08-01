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
import classnames from "classnames";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Spinner} from "views/components/spinner";
import * as Buttons from "../buttons";
import * as styles from "./index.scss";
import {ModalManager} from "./modal_manager";

const uuid4 = require("uuid/v4");

//todo: Remove extraLargeHackForEAProfiles once we fix plugins view to provide the modal container dimensions
export enum Size {small, medium, large, extraLargeHackForEaProfiles}

export enum ModalState {
  OK, LOADING
}

export abstract class Modal extends MithrilViewComponent<any> {
  public id: string;
  private readonly size: Size;
  protected closeModalOnOverlayClick: boolean = true;
  protected modalState                        = ModalState.OK;

  protected constructor(size = Size.medium) {
    super();
    this.id   = `modal-${uuid4()}`;
    this.size = size;
  }

  abstract title(): string;

  abstract body(): m.Children;

  buttons(): m.ChildArray {
    return [<Buttons.Primary data-test-id="button-ok" onclick={this.close.bind(this)}>OK</Buttons.Primary>];
  }

  render() {
    ModalManager.render(this);
  }

  close() {
    ModalManager.close(this);
  }

  closeOnEscape(e: KeyboardEvent) {
    if (e.key === "Escape") {
      this.close();
    }
  }

  oninit() {
    document.body.addEventListener("keydown", this.closeOnEscape.bind(this));
  }

  onremove() {
    document.body.removeEventListener("keydown", this.closeOnEscape.bind(this));
  }

  view() {
    const spinner = this.isLoading() ? (<Spinner/>) : null;
    return <div class={classnames(styles.overlay, Size[this.size])}>
      <header class={styles.overlayHeader}>
        <h3 data-test-id="modal-title">{this.title()}</h3>
        <button class={styles.overlayClose} onclick={this.close.bind(this)}><i class={styles.closeIcon}/>
        </button>
      </header>
      <div
        class={classnames(styles.overlayContent, {[styles.spinnerWrapper]: this.isLoading()})}
        data-test-id="modal-body">
        <div class={classnames({[styles.modalBodyOverlay]: this.isLoading()})}>
          {spinner}
        </div>
        {this.body()}
      </div>
      <footer class={styles.overlayFooter}>
        {
          _.forEach(_.reverse(this.buttons()), (button) => {
            return button;
          })
        }
      </footer>
    </div>;
  }

  shouldCloseModalOnOverlayClick(): boolean {
    return this.closeModalOnOverlayClick;
  }

  protected isLoading() {
    return this.modalState === ModalState.LOADING;
  }
}
