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
import classnames from "classnames";
import {RestyleAttrs, RestyleViewComponent} from "jsx/mithril-component";
import m from "mithril";
import defaultStyles from "./forms.scss";

type Styles = typeof defaultStyles;

interface Attrs extends RestyleAttrs<Styles> {
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

export class Form extends RestyleViewComponent<Styles, Attrs> {
  css = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    return <div class={classnames(this.css.form, (vnode.attrs.compactForm ? this.css.formCompact : this.css.formResponsive), {[this.css.last]: vnode.attrs.last})} {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </div>;
  }
}

export class FormHeader extends RestyleViewComponent<Styles, Attrs> {
  css = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    return <div class={this.css.formHeader} {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </div>;
  }
}

export class FormBody extends RestyleViewComponent<Styles, Attrs> {
  css = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    return <div {...dataTestIdAttrs(vnode.attrs)}>
      {vnode.children}
    </div>;
  }
}
