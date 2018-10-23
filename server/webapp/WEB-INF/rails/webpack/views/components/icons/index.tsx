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

import {MithrilComponent} from "../../../jsx/mithril-component";
import * as m from 'mithril';

const styles     = require('./index.scss');
const classnames = require('classnames/bind').bind(styles);

class Icon<A = {}> extends MithrilComponent<A> {
  private readonly name: string;

  protected constructor(name: string) {
    super();
    this.name = name;
  }

  view(vnode: m.Vnode<A>) {
    return (
      <button className={styles.icon}>
        <i className={classnames(this.name)}/>
      </button>
    );
  }
}

export class Settings extends Icon {
  constructor() {
    super('settings');
  }
}

export class Analytics extends Icon {
  constructor() {
    super('analytics');
  }
}

export class Edit extends Icon {
  constructor() {
    super('edit');
  }
}

export class Clone extends Icon {
  constructor() {
    super('clone');
  }
}

export class Delete extends Icon {
  constructor() {
    super('remove');
  }
}

export class Lock extends Icon {
  constructor() {
    super('lock');
  }
}

export class IconGrop extends MithrilComponent<{}> {
  view(vnode: m.Vnode<{}>) {
    return (
      <div className={styles.iconGroup} arial-label="actions">
        {vnode.children}
      </div>
    );
  }
}
