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

import {bind} from "classnames/bind";
import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as styles from "./forms.scss";

const classnames = bind(styles);

export class Form extends MithrilComponent {

  view(vnode: m.Vnode) {
    return <ul className={classnames(styles.form, styles.formResponsive)}>
      {vnode.children}
    </ul>;
  }
}

export class FormItem extends MithrilComponent {
  view(vnode: m.Vnode) {
    return <li class={styles.formGroup}>
      {vnode.children}
    </li>;
  }
}

export class FormHeader extends MithrilComponent {
  view(vnode: m.Vnode) {
    return <div class={styles.formHeader}>
      {vnode.children}
    </div>;
  }
}

export class FormBody extends MithrilComponent {
  view(vnode: m.Vnode) {
    return <div>
      {vnode.children}
    </div>;
  }
}
