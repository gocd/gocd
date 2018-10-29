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
import {MithrilViewComponent} from "../../../jsx/mithril-component";
import * as styles from './index.scss';

export namespace ModalManager {

  type ModalEntry = { key: string, value: Modal }
  let allModals: ModalEntry[] = [];

  //call this function once when the SPA is loaded
  export function onPageLoad() {
    const $body = $('body');
    const $modalParent = $('<div class="component-modal-container"/>').appendTo($body);
    $modalParent.data('modals', allModals);
    const ModalDialogs = class extends MithrilViewComponent<any> {
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
    m.mount($modalParent.get(0), ModalDialogs);
  }

  export function render(modal: Modal) {
    allModals.push({key: modal.id, value: modal});
    $('body').addClass(styles.fixed);
    m.redraw();
  }

  export function close(modal: Modal) {
    _.remove(allModals, (entry) => entry.key === modal.id);
    $('body').removeClass(styles.fixed);
    m.redraw();
  }
}
