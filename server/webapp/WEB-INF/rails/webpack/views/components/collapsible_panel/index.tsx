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
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import * as styles from "./index.scss";

const classnames = bind(styles);

type AttributeType = m.Children;

export interface Attrs<Header, Actions> {
  actions?: AttributeType | AttributeType[];
  header: AttributeType | string;
  error?: boolean;
}

export interface State {
  toggle: () => void;
  expanded: Stream<boolean>;
}

export class CollapsiblePanel<Header, Actions> extends MithrilComponent<Attrs<Header, Actions>, State> {

  oninit(this: State, vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    vnode.state.expanded = stream(false);
    vnode.state.toggle   = () => {
      vnode.state.expanded(!vnode.state.expanded());
    };
  }

  view(vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    const collapsibleClasses = classnames({
      [styles.expanded]: vnode.state.expanded(),
      [styles.error]: vnode.attrs.error
    });
    let actions;
    if (vnode.attrs.actions) {
      actions = <div class={styles.actions}>
        {vnode.attrs.actions}
      </div>;
    }

    return (
      <div class={classnames(styles.collapse, collapsibleClasses)}>
        <div class={classnames(styles.collapseHeader, collapsibleClasses)}
             data-test-id="collapse-header"
             onclick={vnode.state.toggle}>
          <div class={styles.headerDetails}>
            {vnode.attrs.header}
          </div>
          {actions}
        </div>

        <div data-test-id="collapse-body"
             class={classnames(styles.collapseBody, {[styles.hide]: !vnode.state.expanded()})}>
          {vnode.children}
        </div>
      </div>
    );
  }
}
