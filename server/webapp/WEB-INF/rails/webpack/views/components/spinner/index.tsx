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
import m from "mithril";

import {bind} from "classnames/bind";
import styles from "./index.scss";

const classnames = bind(styles);

import {MithrilViewComponent} from "jsx/mithril-component";

export interface Attrs {
  small?: boolean;
  dataTestId?: string;
}

export class Spinner extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isSmall    = vnode.attrs.small;
    const dataTestId = vnode.attrs.dataTestId ? vnode.attrs.dataTestId : "spinner";

    return (
      <span class={classnames(styles.pageSpinner, {[styles.small]: isSmall})} data-test-id={dataTestId}/>
    );
  }
}
