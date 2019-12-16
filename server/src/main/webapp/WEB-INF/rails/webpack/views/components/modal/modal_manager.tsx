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
import _ from "lodash";
import m from "mithril";
import {Wizard} from "views/components/wizard";
import * as wizardStyles from "../wizard/index.scss";
import {Modal} from "./index";
import styles from "./index.scss";

let modalContainer: Element;

export namespace ModalManager {

  interface ModalEntry {
    key: string;
    value: Modal | Wizard;
  }

  const allModals: ModalEntry[] = [];

  //call this function once when the SPA is loaded
  export function onPageLoad() {
    if (modalContainer) {
      throw new Error("Did you initialize modal manager twice?");
    }
    const body     = document.body;
    modalContainer = document.createElement("div");
    modalContainer.setAttribute("class", "component-modal-container");
    body.appendChild(modalContainer);
    const ModalDialogs = class extends MithrilViewComponent<any> {
      view() {
        return (
          _.map(allModals, (entry) => {
            if (entry.value instanceof Modal) {
              return <div key={entry.key}
                          data-test-id={entry.key}
                          class={styles.overlayBg}
                          onclick={(event: MouseEvent) => overlayBackgroundClicked(event,
                                                                       entry.value as Modal)}>{m(entry.value)}</div>;
            }

            return <div key={entry.key}
                        data-test-id={entry.key}
                        class={wizardStyles.overlay}>{m(entry.value)}</div>;
          })
        );
      }
    };
    m.mount(modalContainer, ModalDialogs);
  }

  export function render(modal: Modal | Wizard) {
    allModals.push({key: modal.id, value: modal});
    document.body.classList.add(styles.fixed);
    m.redraw();
  }

  export function close(modal: Modal | Wizard) {
    _.remove(allModals, (entry) => entry.key === modal.id);
    document.body.classList.remove(styles.fixed);
    m.redraw();
  }

  export function closeAll() {
    _.forEach(allModals, (m: ModalEntry) => {
      ModalManager.close(m.value);
    });
  }

  function overlayBackgroundClicked(event: Event, modal: Modal) {
    const overlayBg = document.getElementsByClassName(styles.overlayBg).item(0);
    if (modal.shouldCloseModalOnOverlayClick() && event.target === overlayBg) {
      modal.close();
    }
  }
}
