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

import {el} from "helpers/dom";
import {RestyleAttrs, RestyleComponent} from "jsx/mithril-component";
import m from "mithril";
import defaultStyles from "./more.scss";

type Styles = typeof defaultStyles;
interface Attrs extends RestyleAttrs<Styles> {
  abstract: m.Children;
  textMore?: string;
  textLess?: string;
}

const DEFAULT_MORE = "(show more)";
const DEFAULT_LESS = "(show less)";

export class ShowMore extends RestyleComponent<Styles, Attrs> {
  css = defaultStyles;

  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    const leadIn = (vnode.dom.firstElementChild as HTMLElement);
    const toggle = el("a", {class: this.css.toggle}, vnode.attrs.textMore || DEFAULT_MORE);

    toggle.addEventListener("click", () => {
      vnode.dom.classList.toggle(this.css.open);
      toggle.textContent = vnode.dom.classList.contains(this.css.open) ?
        (vnode.attrs.textLess || DEFAULT_LESS) : (vnode.attrs.textMore || DEFAULT_MORE);
    });

    leadIn.appendChild(document.createTextNode(" "));
    leadIn.appendChild(toggle);
  }

  view(vnode: m.Vnode<Attrs, {}>) {
    return <dl class={this.css.showMoreDoc}>
      <dt class={this.css.leadIn}>{vnode.attrs.abstract}</dt>
      <dd class={this.css.additional}>{vnode.children}</dd>
    </dl>;
  }
}
