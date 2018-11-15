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
import * as styles from "./index.scss";

const classnames = bind(styles);

type AttributeType = m.Children;

export interface Attrs<Header, Actions> {
  actions?: AttributeType | AttributeType[];
  header: AttributeType | string;
}


export class Panel<Header, Actions> extends MithrilComponent<Attrs<Header, Actions>> {
  view(vnode: m.Vnode<Attrs<Header, Actions>>): m.Children | void | null {
    let actions;
    if (vnode.attrs.actions) {
      actions = <div class={styles.actions} data-test-id="panel-header-actions">
        {vnode.attrs.actions}
      </div>;
    }


    return (
      <div class={classnames(styles.panel)}>
        <div class={classnames(styles.panelHeader)}
             data-test-id="panel-header">
          <div class={styles.headerDetails}>
            {vnode.attrs.header}
          </div>
          {actions}
        </div>

        <div data-test-id="panel-body"
             class={classnames(styles.panelBody)}>
          {vnode.children}
        </div>
      </div>
    );
  }
}
