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

import {MithrilViewComponent} from "jsx/mithril-component";
import m from "mithril";

import styles from "./index.scss";

export interface Attrs {
  target?: string;
  href?: string;
}

export class Link extends MithrilViewComponent<Attrs> {

  maybeRel(attrs: Attrs) {
    if (attrs.target) {
      return {rel: "noopener noreferrer"};
    }
    return {};
  }

  view(vnode: m.Vnode<Attrs>) {
    const rel = this.maybeRel(vnode.attrs);
    return (<a {...vnode.attrs} {...rel} class={styles.inlineLink}>{vnode.children}</a>);
  }
}
