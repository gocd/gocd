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
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import * as styles from "./index.scss";

const classnames = bind(styles);

interface Attrs {
  tabs: m.Child[] | any;
  contents: m.Child[] | any;
}

interface State {
  selectedTabIndex: Stream<number>;
  setSelectedTabIndex: (index: number, e: MouseEvent) => void;
  isSelected: (index: number) => boolean;
}

export class Tabs extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>): any {
    vnode.state.selectedTabIndex = stream(0);

    vnode.state.setSelectedTabIndex = (index: number, e: MouseEvent) => {
      vnode.state.selectedTabIndex(index);
    };

    vnode.state.isSelected = (index: number) => {
      return vnode.state.selectedTabIndex() === index;
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    return <div className={styles.tabs}>
      <ul className={styles.tabHeaderContainer}>
        {vnode.attrs.tabs.map((tab: any, index: number) => {
          return <li className={classnames(styles.tabHeader, {[styles.isActive]: vnode.state.isSelected(index)})}
                     onclick={vnode.state.setSelectedTabIndex.bind(vnode.state, index)}
                     data-test-id={`tab-header-${index}`}>
            {tab}
          </li>;
        })}
      </ul>
      <div>
        {vnode.attrs.contents.map((content: any, index: number) => {
          return <div
            className={classnames(styles.tabContent, {[styles.hide]: !vnode.state.isSelected(index)})}
            data-test-id={`tab-content-${index}`}>
            {content}
          </div>;
        })}
      </div>
    </div>;
  }
}
