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

import {MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as s from "underscore.string";
import * as styles from "./index.scss";

interface Attrs {
  headers: m.Child[];
  data: m.Child[][];
}

export class Table extends MithrilViewComponent<Attrs> {
  view(vnode: m.Vnode<Attrs>) {

    return ( <table className={styles.table} data-test-id="table">
        <thead data-test-id="table-header">
        <tr data-test-id="table-header-row">
          {vnode.attrs.headers.map((header) => (<th >{Table.renderedValue(header)}</th>))}
        </tr>
        </thead>
        <tbody data-test-id="table-body">
        {
          vnode.attrs.data.map((rows) => {
            return (
              <tr data-test-id="table-row">
                {rows.map((row) => <td>{Table.renderedValue(row)}</td>)}
              </tr>
            );
          })
        }
        </tbody>
      </table>
    );
  }

  private static renderedValue(value: m.Children) {
    // check booleans, because they're weird in JS :-/
    if (_.isBoolean(value) || _.isNumber(value)) {
      // toString() because `false` values will not be rendered
      return (value.toString());
    }

    if (_.isNil(value) || _.isEmpty(value)) {
      return this.unspecifiedValue();
    }

    if (_.isString(value) && s.isBlank(value)) {
      return this.unspecifiedValue();
    }

    return value;
  }

  private static unspecifiedValue() {
    return ('');
  }
}
