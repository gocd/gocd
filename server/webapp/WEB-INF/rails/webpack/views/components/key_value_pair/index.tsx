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
  data: Map<string, string | JSX.Element>;
  inline?: boolean;
}

export class KeyValuePair extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {
    const isInline = vnode.attrs.inline;

    const elements: JSX.Element[] = [];
    vnode.attrs.data.forEach((value, key) => {

      const dataTestIdForKey   = `key-value-key-${key.replace(/ /g, "-").toLowerCase() as string}`;
      const dataTestIdForValue = `key-value-value-${key.replace(/ /g, "-").toLowerCase() as string}`;

      elements.push(<li className={classnames(styles.keyValueItem, {[styles.keyValueInlineItem]: isInline})} key={key}>
        <label data-test-id={dataTestIdForKey} className={styles.key}>{key}</label>
        <span data-test-id={dataTestIdForValue}
              className={styles.value}>{KeyValuePair.renderedValue(value)}</span>
      </li>);
    });
    return (
      <ul className={classnames(styles.keyValuePair, {[styles.keyValuePairInline]: isInline})}>
        {elements}
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
