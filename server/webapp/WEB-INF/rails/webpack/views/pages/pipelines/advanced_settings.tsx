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

import {bind} from "classnames/bind";
import {MithrilViewComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as css from "./components.scss";

const cls = bind(css);

interface Attrs {
  forceOpen?: boolean;
}

export class AdvancedSettings extends MithrilViewComponent<Attrs> {
  oncreate(vnode: m.VnodeDOM<Attrs, {}>) {
    const el = vnode.dom;
    el!.querySelector("dt")!.addEventListener("click", () => {
      if (!vnode.attrs.forceOpen) {
        el.classList.toggle(css.open);
      }
    });
  }

  onupdate(vnode: m.VnodeDOM<Attrs, {}>) {
    if (vnode.attrs.forceOpen) {
      vnode.dom.classList.remove(css.open);
    }
  }

  view(vnode: m.Vnode<Attrs>): m.Children | void | null {
    return <dl class={cls(css.advancedSettings, {[css.lockOpen]: vnode.attrs.forceOpen})}>
      <dt class={css.summary}>Advanced Settings</dt>
      <dd class={css.details}>{vnode.children}</dd>
    </dl>;
  }
}
