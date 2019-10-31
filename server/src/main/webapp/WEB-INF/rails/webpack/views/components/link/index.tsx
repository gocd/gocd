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
import m from "mithril";
import styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs {
  target?: string;
  href?: string;
  externalLinkIcon?: boolean;
  onclick?: (e: MouseEvent) => void;
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
    return (<a target={vnode.attrs.target} href={vnode.attrs.href} {...rel}
               onclick={vnode.attrs.onclick}
               class={classnames(styles.inlineLink, {[styles.externalIcon]: vnode.attrs.externalLinkIcon})}>
      {vnode.children}</a>);
  }
}
