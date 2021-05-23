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
  initialSelection?: number;
  tabs: m.Child[] | any;
  contents: m.Child[] | any;
  beforeChange?: (selectedTabIndex: number, done: () => void) => void;
}

interface State {
  selectedTabIndex: Stream<number>;
  isSelected: (index: number) => boolean;
}

export class Tabs extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>): any {
    vnode.state.selectedTabIndex = Stream(vnode.attrs.initialSelection || 0);
    vnode.state.isSelected       = (index: number) => {
      return vnode.state.selectedTabIndex() === index;
    };
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    const tabs = (
      <ul class={styles.tabs}>
        {vnode.attrs.tabs.map((tab: any, index: number) => {
          const classesToApply = classnames(styles.tabHead, {[styles.active]: vnode.state.isSelected(index)});

          return <li onclick={() => {
            const done = () => {
              vnode.state.selectedTabIndex(index);
            };

            if (vnode.attrs.beforeChange) {
              vnode.attrs.beforeChange(index, done);
            } else {
              done();
            }
          }}>
            <a class={classesToApply}
               href={"javascript:void(0);"}
               data-test-id={`tab-header-${index}`}
            >{tab}</a>
          </li>;
        })}
      </ul>);

    const tabContents = (
      <div class={styles.tabContainer}>
        {vnode.attrs.contents.map((content: any, index: number) => {
          return [
            <h3 class={classnames(styles.tabAccordionHeading,
                                  {[styles.active]: vnode.state.isSelected(index)})}
                onclick={() => vnode.state.selectedTabIndex(index)}>
              {vnode.attrs.tabs[index]}
            </h3>,
            <div data-test-id={`tab-content-${index}`}
                 class={classnames(styles.tabContent, {[styles.hide]: !vnode.state.isSelected(index)})}
                 id={`tab${index}`}>
              {content}
            </div>
          ];
        })}
      </div>);
    return <div>
      {tabs}
      {tabContents}
    </div>;
  }
}
