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

import {HTMLAttributes} from "jsx/dom";
import * as _ from "lodash";
import * as m from 'mithril';

import {MithrilViewComponent} from "jsx/mithril-component";

import {bind} from "classnames/bind";
import * as styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs extends HTMLAttributes {
  onclick?: () => void;
  disabled?: boolean;
}

class Icon extends MithrilViewComponent<Attrs> {
  private readonly name: string;
  private readonly title: string;

  protected constructor(name: string, title: string) {
    super();
    this.name = name;
    this.title = title;
  }

  view(vnode: m.Vnode<Attrs>) {
    return (
      <button title={this.title} className={(classnames(styles.btnIcon, {disabled: vnode.attrs.disabled}))}>
        <i {...vnode.attrs} className={classnames(this.name)}/>
      </button>
    );
  }
}

export class Settings extends Icon {
  constructor() {
    super(styles.settings, "Settings");
  }
}

export class Analytics extends Icon {
  constructor() {
    super(styles.analytics, "Analytics");
  }
}

export class Edit extends Icon {
  constructor() {
    super(styles.edit, "Edit");
  }
}

export class Clone extends Icon {
  constructor() {
    super(styles.clone, "Clone");
  }
}

export class Delete extends Icon {
  constructor() {
    super(styles.remove, "Delete");
  }
}

export class Lock extends Icon {
  constructor() {
    super(styles.lock, "Lock");
  }
}

export class Close extends Icon {
  constructor() {
    super(styles.close, "Close");
  }
}

export class QuestionMark extends Icon {
  constructor() {
    super(styles.question, "Help");
  }
}

export class ButtonGroup extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (
      <div className={styles.buttonGroup} aria-label="actions">
        {vnode.children}
      </div>
    );
  }
}
