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
import * as _ from 'lodash';
import * as styles from "./index.scss";
import {MithrilViewComponent} from "jsx/mithril-component";

const classnames = require('classnames/bind').bind(styles);

export interface Attrs {
  data: any
  inline?: boolean
}

export class KeyValuePair extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isInline = vnode.attrs.inline;
    return (
      <ul className={classnames(styles.keyValuePair, {[styles.keyValuePairInline]: isInline})}>
        {
          _.map(vnode.attrs.data, (value, key) => {
            return [
              <li className={classnames(styles.keyValueItem, {[styles.keyValueInlineItem]: isInline})} key={key}>
                <label className={styles.key}>{key}</label>
                <span className={styles.value}>
                  <pre>{value}</pre>
                </span>
              </li>
            ];
          })
        }
      </ul>
    );
  }
}


