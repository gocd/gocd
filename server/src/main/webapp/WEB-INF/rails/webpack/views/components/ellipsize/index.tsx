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

import classNames from "classnames/bind";

import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import styles from "./index.scss";

const classnames = classNames.bind(styles);

interface Attrs {
  text: string;
  size?: number;
  fixed?: boolean;
}

interface State {
  expanded: Stream<boolean>;
  setExpandedTo: (state: boolean, e: MouseEvent) => void;
}

const flag: (val?: boolean) => Stream<boolean> = Stream;

export class Ellipsize extends MithrilComponent<Attrs, State> {
  private static readonly DEFAULT_SIZE: number = 40;

  oninit(vnode: m.Vnode<Attrs, State>): any {
    vnode.state.expanded = flag(false);

    vnode.state.setExpandedTo = (state: boolean) => {
      vnode.state.expanded(state);
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    if (!vnode.attrs.text) {
      return <span class={styles.wrapper}>{vnode.attrs.text}</span>;
    }

    const charactersToShow = Math.min((vnode.attrs.size || Ellipsize.DEFAULT_SIZE), vnode.attrs.text.length);

    if (vnode.attrs.fixed) {
      return <span class={classnames(styles.wrapper, styles.fixEllipsized)}>{vnode.attrs.text}</span>;
    }

    if (Ellipsize.ellipsisNotRequired(vnode)) {
      return <span class={styles.wrapper}>{vnode.attrs.text}</span>;
    }

    return <span class={styles.wrapper}>
      <span
        data-test-id="ellipsized-content">
        {vnode.state.expanded() ? vnode.attrs.text : Ellipsize.getEllipsizedString(vnode, charactersToShow)}
      </span>
      {vnode.state.expanded() ? Ellipsize.element(vnode, "less", false) : Ellipsize.element(vnode, "more", true)}
      </span>;

  }

  private static ellipsisNotRequired(vnode: m.Vnode<Attrs, State>) {
    return vnode.attrs.text.length < Math.min(vnode.attrs.size || Ellipsize.DEFAULT_SIZE);
  }

  private static getEllipsizedString(vnode: m.Vnode<Attrs, State>, charactersToShow: number) {
    return vnode.attrs.text.substr(0, charactersToShow).concat("...");
  }

  private static element(vnode: m.Vnode<Attrs, State>, text: string, state: boolean) {
    return <span class={styles.ellipsisActionButton}
                 onclick={vnode.state.setExpandedTo.bind(this, state)}>{text}</span>;
  }
}
