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
import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as styles from "./index.scss";

const classnames = bind(styles);

export interface Attrs {
  // pass in an object if you do not care about order, pass an array of key-value pairs if you care about order
  data: { [key: string]: m.Children } | m.Children[];
  inline?: boolean;
}

export class KeyValuePair extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isInline = vnode.attrs.inline;
    return (
      <ul className={classnames(styles.keyValuePair, {[styles.keyValuePairInline]: isInline})}>
        {
          _.map(vnode.attrs.data, (...args) => {
            let value, key;
            if (_.isArray(vnode.attrs.data)) {
              [key, value] = args[0] as any[];
            } else {
              [value, key] = args;
            }
            return [
              <li className={classnames(styles.keyValueItem, {[styles.keyValueInlineItem]: isInline})}
                  key={key as string}>
                <label data-test-id={`key-value-key-${key as string}`} className={styles.key}>{key}</label>
                <span data-test-id={`key-value-value-${key as string}`}
                      className={styles.value}>{KeyValuePair.renderedValue(value)}</span>
              </li>
            ];
          })
        }
      </ul>
    );
  }

  private static renderedValue(value: any) {
    if (_.isNil(value) || _.isEmpty(value)) {
      return (<em>(Not specified)</em>);
    }

    // performat some "primitive" types
    if (_.isString(value) || _.isBoolean(value) || _.isNumber(value)) {
      return (<pre>{value}</pre>);
    }
    return value;
  }
}
