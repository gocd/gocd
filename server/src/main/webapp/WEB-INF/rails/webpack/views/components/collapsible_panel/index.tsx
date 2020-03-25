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
import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import styles from "./index.scss";

interface CollapsibleStateModel {
  expanded: (newVal?: boolean) => boolean;
}

interface Attrs {
  dataTestId?: string;
  actions?: m.Children;
  header: m.Children;
  error?: boolean;
  warning?: boolean;

  // this property is a bit misleading. it only controls
  // the initial state, but after init(), the expand/collapse
  // is only controllable from user interaction. a redraw()
  // with `expanded = false` does nothing.
  expanded?: boolean;

  // allows us to programmatically inspect the state and control
  // the state from outside
  vm?: CollapsibleStateModel;

  onexpand?: () => void;
  oncollapse?: () => void;

  //adding to prevent any user interactions
  nonExpandable?: boolean
}

interface State extends CollapsibleStateModel {
  toggle: () => void;
  fireEvents: (expanded: boolean) => void;
}

export class CollapsiblePanel extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>) {
    const defaultState = Stream(false);

    vnode.state.expanded = (val?: boolean) => {
      const {vm} = vnode.attrs; // Don't optimize! Read from attrs within the method body because vm instance may be different on subsequent invocations
      const state = vm ? vm.expanded : defaultState;

      if (void 0 !== val) {
        state(val);
        vnode.state.fireEvents(val);
      }

      return state();
    };

    vnode.state.fireEvents = (expanded: boolean) => {
      if (expanded) {
        if ("function" === typeof vnode.attrs.onexpand) {
          vnode.attrs.onexpand();
        }
      } else {
        if ("function" === typeof vnode.attrs.oncollapse) {
          vnode.attrs.oncollapse();
        }
      }
    };

    vnode.state.toggle = () => {
      vnode.state.expanded(!vnode.state.expanded());
    };

    if (void 0 !== vnode.attrs.expanded) { // set the initial value, if provided
      vnode.state.expanded(vnode.attrs.expanded);
    } else {
      vnode.state.fireEvents(vnode.state.expanded());
    }
  }

  view(vnode: m.Vnode<Attrs, State>) {
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

    const expandCollapseState = vnode.attrs.nonExpandable || vnode.state.expanded() ? "expanded" : "collapsed";
    const toggle              = vnode.attrs.nonExpandable ? _.noop : vnode.state.toggle;
    const header              = vnode.attrs.nonExpandable ? styles.nonCollapseHeader : styles.collapseHeader;

    return (
      <div data-test-id={vnode.attrs.dataTestId}
           data-test-element-state={expandCollapseState}
           data-test-has-error={vnode.attrs.error}
           data-test-has-warning={vnode.attrs.warning}
           class={classnames(styles.collapse, collapsibleClasses)}>
        <div class={classnames(header, collapsibleClasses)}
             data-test-id="collapse-header"
             onclick={toggle}>
          <div class={styles.headerDetails}>
            {vnode.attrs.header}
          </div>
          {actions}
        </div>

        <div data-test-id="collapse-body"
             class={classnames(styles.collapseBody, {[styles.hide]: expandCollapseState === "collapsed"})}>
          {vnode.children}
        </div>
      </div>
    );
  }
}
