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
import * as styles from './index.scss';
import {MithrilComponent} from "../../../jsx/mithril-component";

export interface Attrs {
  data: any
}

export class KeyValuePair extends MithrilComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    return (<ul className={styles.keyValuePair}>
      {
        _.map(vnode.attrs.data, (value, key) => {
          return [
            <li>
              <label className={styles.key}>{key}</label>
              <span className={styles.value}>
                  <pre>{value}</pre>
                </span>
            </li>
          ];
        })
      }
    </ul>);
  }
}


