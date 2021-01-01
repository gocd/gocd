/*
 * Copyright 2021 ThoughtWorks, Inc.
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
  disabled?: boolean;
  target?: string;
  href?: string;
  externalLinkIcon?: boolean;
  onclick?: (e: MouseEvent) => void;
  title?: string;
  dataTestId?: string;
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
    return (<a target={vnode.attrs.target}
               href={vnode.attrs.disabled ? "javascript:void(0)" : vnode.attrs.href}
               {...rel}
               data-test-id={vnode.attrs.dataTestId}
               onclick={vnode.attrs.disabled ? () => false : vnode.attrs.onclick}
               disabled={vnode.attrs.disabled}
               class={classnames(styles.inlineLink, {[styles.externalIcon]: vnode.attrs.externalLinkIcon})}
               title={vnode.attrs.title}>
      {vnode.children}</a>);
  }
}
