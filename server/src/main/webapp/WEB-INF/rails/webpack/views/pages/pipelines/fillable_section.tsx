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
import {RestyleAttrs, RestyleComponent} from "jsx/mithril-component";
import m from "mithril";
import defaultStyles from "./fillable_section.scss";

type Styles = typeof defaultStyles;

interface Attrs extends RestyleAttrs<Styles> {
  hasSubsections?: boolean;
}

export class FillableSection extends RestyleComponent<Styles, Attrs> {
  css = defaultStyles;

  view(vnode: m.Vnode<Attrs>) {
    return <article class={classnames(this.css.fillable, { [this.css.multiSection]: vnode.attrs.hasSubsections })}>
      {vnode.children}
    </article>;
  }
}
