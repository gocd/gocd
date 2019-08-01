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
import * as m from "mithril";
import * as styles from "./forms.scss";

interface Attrs {
  dataTestId?: string;
  compactForm?: boolean;
  last?: boolean;
}

function dataTestIdAttrs(attrs: Attrs) {
  if (attrs.dataTestId) {
    return {
      "data-test-id": attrs.dataTestId
    };
  } else {
    return {};
  }
}

export class Form extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <ul class={classnames(styles.form, (vnode.attrs.compactForm ? styles.formCompact : styles.formResponsive), {[styles.last]: vnode.attrs.last})} {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </ul>;
  }
}

export class FormHeader extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div class={styles.formHeader} {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </div>;
  }
}

export class FormBody extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </div>;
  }
}
