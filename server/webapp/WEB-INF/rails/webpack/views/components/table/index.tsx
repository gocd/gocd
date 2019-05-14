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
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import * as _ from "lodash";
import * as m from "mithril";
import * as s from "underscore.string";
import * as styles from "./index.scss";

const classnames = bind(styles);

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

interface State {
  dragging: number;
  dragStart: (e: any) => void;
  dragOver: (e: any) => void;
  dragEnd: () => void;
  dragged: number;
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

export class Table extends MithrilComponent<Attrs, State> {

  oninit(vnode: m.Vnode<Attrs, State>): any {

    // Return if not draggable
    if (!vnode.attrs.draggable) {
      return;
    }

    vnode.state.dragStart = (e) => {
      vnode.state.dragged          = Number(e.currentTarget.dataset.id);
      vnode.state.dragging         = vnode.state.dragged;
      e.dataTransfer.effectAllowed = "move";
      e.dataTransfer.setData("text/html", null);
    };

    vnode.state.dragOver = (e) => {
      e.preventDefault();

      const toBeReplaced                 = e.target;
      const updatedPositionWhileDragging = vnode.state.dragging;
      const newPosition                  = Number(toBeReplaced.dataset.id);

      if (updatedPositionWhileDragging === newPosition) {
        return;
      }

      // deleting the dragged element from the old position
      const draggedElement = vnode.attrs.data.splice(updatedPositionWhileDragging, 1)[0];
      // moving the element to new position
      vnode.attrs.data.splice(newPosition, 0, draggedElement);
      vnode.state.dragging = newPosition;
      if (vnode.attrs.dragHandler) {
        vnode.attrs.dragHandler(updatedPositionWhileDragging, newPosition);
      }
    };

    vnode.state.dragEnd = () => {
      vnode.state.dragging = -1;
      m.redraw();
    };
  }

  view(vnode: m.Vnode<Attrs, State>) {
    let draggableColHeader: m.Child;
    let tableCss: string | undefined;

    if (vnode.attrs.draggable) {
      draggableColHeader = <th></th>;
      tableCss           = styles.draggable;
    }
    return <table className={classnames(styles.table, tableCss)}
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
      <tbody data-test-id="table-body">
      {
        _.map(vnode.attrs.data, ((rows, index) => {
          const dragging = (Number(index) === vnode.state.dragging) ? styles.draggableOver : undefined;
          return (
            <tr key={index.toString()}
                data-id={index}
                class={dragging}
                draggable={true}
                ondragstart={vnode.attrs.draggable ? vnode.state.dragStart.bind(this) : Table.disableEvent.bind(this)}
                ondragover={vnode.attrs.draggable ? vnode.state.dragOver.bind(this) : Table.disableEvent.bind(this)}
                ondragend={vnode.attrs.draggable ? vnode.state.dragEnd.bind(this) : Table.disableEvent.bind(this)}
                data-test-id="table-row">
              {vnode.attrs.draggable ?
                <td
                  data-id={index}
                  onmouseover={Table.disableEvent.bind(this)}>
                  <i className={styles.dragIcon}></i>
                </td> : null}
              {_.map(rows,
                     ((row) => <td>{Table.renderedValue(row)}</td>))}
            </tr>
          );
        }))
      }
      </tbody>
    </table>;
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

  private static disableEvent(e: Event) {
    e.preventDefault();
    e.stopPropagation();
    e.stopImmediatePropagation();
    return false;
  }
}
