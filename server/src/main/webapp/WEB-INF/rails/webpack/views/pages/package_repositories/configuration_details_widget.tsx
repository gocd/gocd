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

import {bind} from "classnames/bind";
import {MithrilComponent} from "jsx/mithril-component";
import m from "mithril";
import Stream from "mithril/stream";
import {KeyValuePair} from "views/components/key_value_pair";
import styles from "./index.scss";

const classnames = bind(styles);

interface State {
  packageRepoDetailsExpanded: Stream<boolean>;
}

const flag: (val?: boolean) => Stream<boolean> = Stream;

interface Attrs {
  header: m.Children;
  data: Map<string, m.Children>;
}

export class ConfigurationDetailsWidget extends MithrilComponent<Attrs, State> {
  oninit(vnode: m.Vnode<Attrs, State>): any {
    vnode.state.packageRepoDetailsExpanded = flag(false);
  }

  view(vnode: m.Vnode<Attrs, State>): m.Children | void | null {
    const toggleDetails = (e: MouseEvent) => {
      e.stopPropagation();
      vnode.state.packageRepoDetailsExpanded(!vnode.state.packageRepoDetailsExpanded());
    };

    return (
      <div data-test-id="configuration-details-widget" className={styles.configurationDetailsContainer}>
        <h5
          class={classnames(styles.configurationDetailsHeader, {[styles.expanded]: vnode.state.packageRepoDetailsExpanded()})}
          onclick={toggleDetails.bind(this)}
          data-test-id="configuration-details-header">
          {vnode.attrs.header}
        </h5>
        <div
          class={classnames(styles.configurationDetails, {[styles.expanded]: vnode.state.packageRepoDetailsExpanded()})}
          data-test-id="configuration-details">
          <KeyValuePair data={vnode.attrs.data}/>
        </div>
      </div>
    );
  }
}
