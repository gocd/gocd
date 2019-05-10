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
import * as s from "underscore.string";
import * as styles from "./index.scss";

const classnames = bind(styles);
const Sortable   = require("@shopify/draggable/lib/es5/sortable").default; // es5 bundle => IE11 support

export abstract class TableSortHandler {
  private __currentSortedColumnIndex: number = -1;

  readonly onColumnHeaderClick = (ci: number) => {
    this.__currentSortedColumnIndex = ci;
    this.onColumnClick(ci);
    //tslint:disable-next-line
  };

  abstract getSortableColumns(): number[];

  abstract onColumnClick(columnIndex: number): void;

  currentSortedColumnIndex(): number {
    return this.__currentSortedColumnIndex;
  }
}

interface Attrs {
  headers: any;
  data: m.Child[][];
  "data-test-id"?: string;
  sortHandler?: TableSortHandler;
  draggable?: boolean;
  dragHandler?: (oldIndex: number, newIndex: number) => void;
}

interface HeaderAttrs {
  name: any;
  columnIndex: number;
  width?: string;
  sortCallBackHandler?: TableSortHandler;
}

class TableHeader extends MithrilViewComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs>): m.Children | void | null {
    return TableHeader.sortButton(vnode);
  }

  private static sortButton(vnode: m.Vnode<HeaderAttrs>) {
    if (TableHeader.isSortable(vnode)) {
      return <th className={styles.sortableColumn}
                 onclick={() => vnode.attrs.sortCallBackHandler!.onColumnHeaderClick(vnode.attrs.columnIndex)}>
        {vnode.attrs.name}
        <span className={classnames(styles.sortButton,
                                    {[styles.inActive]: !TableHeader.isSortedByCurrentColumn(vnode)})}>
          <i class="fas fa-sort"/>
      </span></th>;
    }

    return <th>{vnode.attrs.name}</th>;
  }

  private static isSortable(vnode: m.Vnode<HeaderAttrs>) {
    return vnode.attrs.sortCallBackHandler && vnode.attrs.sortCallBackHandler.getSortableColumns()
                                                   .indexOf(vnode.attrs.columnIndex) !== -1;
  }

  private static isSortedByCurrentColumn(vnode: m.Vnode<HeaderAttrs>) {
    if (!vnode.attrs.sortCallBackHandler || vnode.attrs.sortCallBackHandler.currentSortedColumnIndex() === -1) {
      return false;
    }

    return vnode.attrs.sortCallBackHandler.currentSortedColumnIndex() === vnode.attrs.columnIndex;
  }
}

export class Table extends MithrilViewComponent<Attrs> {
  sort: any;
  draggable: boolean = false;
  dragHandler?: (oldIndex: number, newIndex: number) => void;

  oninit(vnode: m.Vnode<Attrs, this>): any {
    this.dragHandler = vnode.attrs.dragHandler;
    this.draggable   = (vnode.attrs.draggable && vnode.attrs.draggable === true) || false;
  }

  view(vnode: m.Vnode<Attrs, this>): m.Children | void | null {
    let draggableColHeader: m.Child;
    let draggableCol: m.Child;
    let iconDrag: string | undefined;
    let draggable_row: string | undefined;
    let draggable_table: string | undefined;

    if (this.draggable) {
      iconDrag           = "icon-drag";
      draggable_row      = "draggable-row";
      draggable_table    = "draggable";
      draggableColHeader = <th></th>;
      draggableCol       =
        <td><i className={classnames(styles.dragIcon, iconDrag)}></i></td>;
    }

    const tableRows = vnode.attrs.data.map((rows) => {
      const index = "a";
      return (
        <tr key={index} class={draggable_row} data-test-id="table-row">
          {draggableCol}
          {rows.map((row) => <td>{Table.renderedValue(row)}</td>)}
        </tr>
      );
    });

    return <table className={classnames(styles.table, draggable_table)}
                  data-test-id={vnode.attrs["data-test-id"] || "table"}>
      <thead data-test-id="table-header">
      <tr data-test-id="table-header-row">
        {draggableColHeader}
        {vnode.attrs.headers
              .map((header: any, index: number) => {
                return <TableHeader name={Table.renderedValue(header)}
                                    columnIndex={index}
                                    sortCallBackHandler={vnode.attrs.sortHandler}/>;
              })
        }
      </tr>
      </thead>
      {/*<tbody data-test-id="table-body">*/}
      {tableRows}
      {/*</tbody>*/}
    </table>;
  }

  oncreate(vnode: m.VnodeDOM<Attrs, this>): any {
    this.initializeSortable(vnode);
  }

  onupdate(vnode: m.VnodeDOM<Attrs, this>): any {
    this.initializeSortable(vnode);
  }

  onremove(vnode: m.VnodeDOM<Attrs, this>): any {
    if (this.sort) {
      this.sort.destroy();
    }
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
    return ("");
  }

  private initializeSortable(vnode: m.VnodeDOM<Attrs, this>) {
    if (!this.sort && this.draggable) {
      this.sort = new Sortable(vnode.dom,
                               {
                                 draggable: ".draggable-row",
                                 handle: ".icon-drag",
                                 classes: {
                                   "mirror": styles.mirror,
                                   "draggable:over": styles.draggableOver
                                 },
                                 mirror: {
                                   appendTo: "body"
                                 }
                               });

      this.sort.on("sortable:stop", (event: any) => {
        if (this.dragHandler) {
          this.dragHandler(event.data.oldIndex, event.data.newIndex);
        }
      });
      return this.sort;
    }
  }
}
