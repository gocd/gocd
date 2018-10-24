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

import * as m from 'mithril';
import * as stream from 'mithril/stream';
import {Stream} from 'mithril/stream';
import * as styles from './index.scss';
import {MithrilComponent} from "../../../jsx/mithril-component";

const classnames = require('classnames/bind').bind(styles);

type AttributeType = JSX.Element | m.Component | undefined;

export interface Attrs<Header, Actions> {
  actions?: AttributeType | Array<AttributeType>;
  header: AttributeType | string;
}

export interface State {
  toggle: () => void;
  expanded: Stream<boolean>;
}

export class CollapsiblePanel<Header, Actions> extends MithrilComponent<Attrs<Header, Actions>, State> {

  oninit(this: State, vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    vnode.state.expanded = stream(false);
    vnode.state.toggle   = function () {
      vnode.state.expanded(!vnode.state.expanded());
    };
  }

  view(vnode: m.Vnode<Attrs<Header, Actions>, State>) {
    const collapsibleClasses = classnames(vnode.state.expanded() && styles.expanded);

    return (
      <div class={classnames(styles.collapse, collapsibleClasses)}>
        <div class={classnames(styles.collapseHeader, collapsibleClasses)}
             data-test-id="collapse-header"
             onclick={vnode.state.toggle}>
          <div class={styles.headerDetails}>
            {vnode.attrs.header}
          </div>
          <div class={styles.actions}>
            {vnode.attrs.actions}
          </div>
        </div>

        <div data-test-id="collapse-body"
             class={classnames(styles.collapseBody, {[styles.hide]: !vnode.state.expanded()})}>
          {vnode.children}
        </div>
      </div>
    );
  }
}
