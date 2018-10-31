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


import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from 'mithril';

import * as styles from './index.scss';
import {bind} from 'classnames/bind';

const classnames = bind(styles);

export interface Attrs {
  small?: boolean,
  onclick?: Function,
  disabled?: boolean
}

class Button extends MithrilViewComponent<Attrs> {
  private readonly type: string;

  protected constructor(type: string) {
    super();
    this.type = type;
  }

  view(vnode: m.Vnode<Attrs>) {
    const isSmall = vnode.attrs.small;

    const onclick: Function = (evt: any) => {
      evt.stopPropagation();
      if (vnode.attrs.disabled) {
        return
      }

      vnode.attrs.onclick && vnode.attrs.onclick(evt);
    };

    return (
      <button {...vnode.attrs}
              onclick={onclick}
              className={classnames(
                styles.button,
                {[styles.btnSmall]: isSmall},
                this.type
              )}>
        {vnode.children}
      </button>
    );
  }
}

export class Primary extends Button {
  constructor() {
    super(styles.btnPrimary);
  }
}

export class Secondary extends Button {
  constructor() {
    super(styles.btnSecondary);
  }
}

export class Reset extends Button {
  constructor() {
    super(styles.btnReset);
  }
}

export class Cancel extends Button {
  constructor() {
    super(styles.btnCancel);
  }
}
