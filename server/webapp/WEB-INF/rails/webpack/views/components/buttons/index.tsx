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


import {MithrilViewComponent} from "../../../jsx/mithril-component";
import * as m from 'mithril';

const styles     = require('./index.scss');
const classnames = require('classnames/bind').bind(styles);

export interface Attrs {
  small?: boolean,
  disabled?: boolean;
  onclick?: Function;
}

class Button extends MithrilViewComponent<Attrs> {
  private readonly type: string;

  protected constructor(type: string) {
    super();
    this.type = type;
  }

  view(vnode: m.Vnode<Attrs>) {
    const isSmall = vnode.attrs.small;

    return (
      <button {...vnode.attrs}
              className={classnames(
                styles.button,
                {[styles.btnSmall]: isSmall},
                {[styles.disabled]: vnode.attrs.disabled},
                styles[this.type]
              )}>
        {vnode.children}
      </button>
    );
  }
}

export class Primary extends Button {
  constructor() {
    super('btn-primary');
  }
}

export class Secondary extends Button {
  constructor() {
    super('btn-secondary');
  }
}

export class Reset extends Button {
  constructor() {
    super('btn-reset');
  }
}

export class Cancel extends Button {
  constructor() {
    super('btn-cancel');
  }
}
