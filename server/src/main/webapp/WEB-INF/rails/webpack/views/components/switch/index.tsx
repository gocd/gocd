/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import m from "mithril";
import Stream from "mithril/stream";

import classnames from "classnames";
import styles from "./index.scss";

import { v4 as uuid4 } from 'uuid';

export interface Attrs {
  inProgress?: boolean;
  small?: boolean;
  disabled?: boolean;
  label?: m.Children;
  onclick?: (e: MouseEvent) => any;
  field: (newValue?: any) => any | Stream<boolean>;
}

export class SwitchBtn extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const switchId   = `switch-${uuid4()}`;
    let label        = null;
    if (vnode.attrs.label) {
      label = <label for={switchId} class={classnames({[styles.disabled]: vnode.attrs.disabled}, styles.switchLabel)}
                     data-test-id="switch-label">
        {vnode.attrs.label}
      </label>;
    }

    return (
      <div class={classnames({[styles.switchSmall]: isSmall}, styles.switchBtn)} data-test-id="switch-wrapper">
        {label}
        <input id={switchId} type="checkbox"
               {...vnode.attrs}
               checked={vnode.attrs.field()}
               onclick={(e: MouseEvent) => {
                 if (vnode.attrs.onclick) {
                   vnode.attrs.onclick(e);
                 }

                 const target = e.target as HTMLInputElement;
                 vnode.attrs.field(target.checked);
               }}
               class={styles.switchInput}
               data-test-id="switch-checkbox"/>
        <label for={switchId} class={classnames({
                                                      [styles.inProgress]: vnode.attrs.field() && vnode.attrs.inProgress,
                                                      [styles.isSuccess]: vnode.attrs.field() && !vnode.attrs.inProgress,
                                                      [styles.disabled]: vnode.attrs.disabled
                                                    }, styles.switchPaddle)} data-test-id="switch-paddle"/>
      </div>);
  }
}
