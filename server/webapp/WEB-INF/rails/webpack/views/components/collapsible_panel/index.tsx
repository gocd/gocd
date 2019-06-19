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
import {MithrilComponent} from "jsx/mithril-component";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import * as styles from "./index.scss";

const classnames = bind(styles);

type AttributeType = m.Children;

export interface Attrs<Header, Actions> {
  dataTestId?: string;
  actions?: AttributeType | AttributeType[];
  header: AttributeType | string;
  error?: boolean;
  warning?: boolean;
  expanded?: boolean;
  onexpand?: () => void;
  oncollapse?: () => void;
}

export interface State {
  toggle: () => void;
  expanded: Stream<boolean>;
}

export class CollapsiblePanel<Header, Actions> extends MithrilComponent<Attrs<Header, Actions>, State> {
  oninit(this: State, vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    vnode.state.expanded = stream(vnode.attrs.expanded || false);
    vnode.state.toggle   = () => {
      vnode.state.expanded(!vnode.state.expanded());
      if (vnode.state.expanded()) {
        if ("function" === typeof vnode.attrs.onexpand) {
          vnode.attrs.onexpand();
        }
      } else {
        if ("function" === typeof vnode.attrs.oncollapse) {
          vnode.attrs.oncollapse();
        }
      }
    };
  }

  view(vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    const collapsibleClasses = classnames({
      [styles.expanded]: vnode.state.expanded(),
      [styles.error]: vnode.attrs.error,
      [styles.warning]: vnode.attrs.warning
    });
    let actions;
    if (vnode.attrs.actions) {
      actions = <div class={styles.actions}>
        {vnode.attrs.actions}
      </div>;
    }

    const expandCollapseState = vnode.state.expanded() ? "expanded" : "collapsed";

    return (
      <div data-test-id={vnode.attrs.dataTestId}
           data-test-element-state={expandCollapseState}
           data-test-has-error={vnode.attrs.error}
           data-test-has-warning={vnode.attrs.warning}
           class={classnames(styles.collapse, collapsibleClasses)}>
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
