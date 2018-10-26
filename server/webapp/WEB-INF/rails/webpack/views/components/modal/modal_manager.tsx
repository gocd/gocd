/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import {Modal} from "./index";
import * as m from 'mithril';
import * as _ from 'lodash';
import {MithrilComponent} from "../../../jsx/mithril-component";
import * as styles from './index.scss';

export namespace ModalManager {

  type ModalEntry = { key: string, value: Modal }
  let allModals: ModalEntry[] = [];

  //call this function once when the SPA is loaded
  export function onPageLoad() {
    const modalParent:Element = document.createElement('div');
    modalParent.classList.add('component-modal-container');
    document.body.append(modalParent);
    const ModalDialogs = class extends MithrilComponent<any> {
      view() {
        return (
          <div>
            {_.map(allModals, (entry) => {
              return <div class={styles.overlayBg}>{m(entry.value)}</div>
            })}
          </div>
        );
      }
    };
    m.mount(modalParent, ModalDialogs);
  }

  export function render(modal: Modal) {
    allModals.push({key: modal.id, value: modal});
    document.body.classList.add(styles.fixed);
    m.redraw();
  }

  export function close(modal: Modal) {
    _.remove(allModals, (entry) => entry.key === modal.id);
    document.body.classList.remove(styles.fixed);
    m.redraw();
  }
}
