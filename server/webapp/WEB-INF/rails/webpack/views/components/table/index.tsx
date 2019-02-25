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

import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import * as s from "underscore.string";
import * as styles from "./index.scss";

export abstract class Comparator<T> {
  protected readonly data: Stream<T[]>;

  protected constructor(data: Stream<T[]>) {
    this.data = data;
  }

  abstract compare(element1: T, element2: T): number;

  readonly sort = (reverse: boolean) => this.data(this.data().sort(this.getFn.bind(this, reverse)));

  private getFn(direction: boolean, element1: T, element2: T) {
    const result = this.compare(element1, element2);
    return direction ? result : result * -1;
  }
}

interface Attrs {
  headers: any;
  data: m.Child[][];
  "data-test-id"?: string;
}

interface HeaderAttrs {
  name: string;
  comparator?: Comparator<any>;
  width?: string;
}

interface DefaultAttrs {
  [key: string]: string | boolean;
}

interface HeaderState {
  sortDirectionToggle: Stream<boolean>;
}

export class TableHeader extends MithrilComponent<HeaderAttrs, HeaderState> {
  oninit(vnode: m.Vnode<HeaderAttrs, HeaderState>): any {
    vnode.state.sortDirectionToggle = stream(false);
  }

  view(vnode: m.Vnode<HeaderAttrs, HeaderState>): m.Children | void | null {
    const attributes: DefaultAttrs = {};
    if (vnode.attrs.width) {
      attributes.width = vnode.attrs.width;
    }

    return <th {...attributes}>
      {Table.renderedValue(vnode.attrs.name)}
      {TableHeader.sortButton(vnode)}
    </th>;
  }

  private static sortButton(vnode: m.Vnode<HeaderAttrs, HeaderState>) {
    if (vnode.attrs.comparator) {
      return <span onclick={() => TableHeader.doSort(vnode)} className={styles.sortButton}>
          <i class="fas fa-sort"/>
        </span>;
    }
  }

  private static doSort(vnode: m.Vnode<HeaderAttrs, HeaderState>) {
    vnode.state.sortDirectionToggle(!vnode.state.sortDirectionToggle());
    vnode.attrs.comparator!.sort(vnode.state.sortDirectionToggle());
  }
}

export class Table extends MithrilViewComponent<Attrs> {
  static renderedValue(value: m.Children) {
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

  view(vnode: m.Vnode<Attrs>) {
    return (<table className={styles.table} data-test-id={vnode.attrs["data-test-id"] || "table"}>
        <thead data-test-id="table-header">
        <tr data-test-id="table-header-row">
          {vnode.attrs.headers.map(Table.wrapInTHTagIfRequired)}
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

  private static unspecifiedValue() {
    return ("");
  }

  private static wrapInTHTagIfRequired(header: any) {
    if (header.tag && header.tag.name === "TableHeader") {
      return header;
    }
    return <th>{Table.renderedValue(header)}</th>;
  }
}
