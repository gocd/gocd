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
import {MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import s from "underscore.string";
import styles from "./index.scss";

const classnames = classNames.bind(styles);

export interface CountsAttr {
  label: string;
  count: number;
  color?: "green" | "red";
}

export interface Attrs {
  dataTestId: string;
  counts: CountsAttr[];
}

export class Counts extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return <div class={classnames(styles.countWrapper)}>
      <ul data-test-id={vnode.attrs.dataTestId}>
        {
          _.map(vnode.attrs.counts, (count) => {
            // @ts-ignore
            const classNames = classnames(styles.count, styles[count.color]);

            return <li>{count.label}: <span data-test-id={`${vnode.attrs.dataTestId}-${s.slugify(count.label)}`}
                                            class={classNames}>{count.count}</span></li>;
          })
        }
      </ul>
    </div>;
  }
}
