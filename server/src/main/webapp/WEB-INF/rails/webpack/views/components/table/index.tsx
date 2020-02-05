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
import {bind} from "classnames/bind";
import {MithrilComponent, MithrilViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import s from "underscore.string";
import styles from "./index.scss";

const classnames = bind(styles);

export enum SortOrder {
  ASC, DESC
}

export interface TableSortHandler {
  getSortableColumns(): number[];

  getCurrentSortOrder(): SortOrder;

  onColumnClick(columnIndex: number): void;
}

interface Attrs {
  headers: m.Child[];
  caption?: m.Children;
  data: m.Child[][];
  "data-test-id"?: string;
  sortHandler?: TableSortHandler;
  draggable?: boolean;
  dragHandler?: (oldIndex: number, newIndex: number) => void;
}

interface HeaderAttrs {
  name: any;
  columnIndex: number;
  currentSortedColumnIndex: Stream<number>;
  width?: string;
  sortCallBackHandler?: TableSortHandler;
}

interface State {
  currentSortedColumnIndex: Stream<number>;
  dragging: number;
  dragStart: (e: DragEvent) => void;
  dragOver: (e: DragEvent) => void;
  dragEnd: () => void;
  dragged: number;
}

class TableHeader extends MithrilViewComponent<HeaderAttrs> {
  view(vnode: m.Vnode<HeaderAttrs>): m.Children | void | null {
    return TableHeader.sortButton(vnode);
  }

  private static sortButton(vnode: m.Vnode<HeaderAttrs>) {
    if (TableHeader.isSortable(vnode)) {
      return <th class={styles.sortableColumn}
                 onclick={() => {
                   vnode.attrs.currentSortedColumnIndex(vnode.attrs.columnIndex);
                   vnode.attrs.sortCallBackHandler!.onColumnClick(vnode.attrs.columnIndex);
                 }}>
        {vnode.attrs.name}
        <span class={TableHeader.getClassesForSortIcon(vnode)}>
          <i class="fas fa-sort"/>
      </span></th>;
    }

    return <th>{vnode.attrs.name}</th>;
  }

  private static getClassesForSortIcon(vnode: m.Vnode<HeaderAttrs>) {
    if (!TableHeader.isSortedByCurrentColumn(vnode)) {
      return classnames(styles.sortButton,
                        {[styles.inActive]: !TableHeader.isSortedByCurrentColumn(vnode)});
    }
    const currentSortOrder = vnode.attrs.sortCallBackHandler!.getCurrentSortOrder();
    return classnames(styles.sortButton
      , {[styles.sortButtonAsc]: currentSortOrder === SortOrder.ASC}
      , {[styles.sortButtonDesc]: currentSortOrder === SortOrder.DESC}
      , {[styles.inActive]: !TableHeader.isSortedByCurrentColumn(vnode)});
  }

  private static isSortable(vnode: m.Vnode<HeaderAttrs>) {
    return vnode.attrs.sortCallBackHandler &&
      vnode.attrs.sortCallBackHandler.getSortableColumns().indexOf(vnode.attrs.columnIndex) !== -1;
  }

  private static isSortedByCurrentColumn(vnode: m.Vnode<HeaderAttrs>) {
    if (vnode.attrs.currentSortedColumnIndex() === -1) {
      return false;
    }

    return vnode.attrs.currentSortedColumnIndex() === vnode.attrs.columnIndex;
  }
}

export class Table extends MithrilComponent<Attrs, State> {

  oninit(vnode: m.Vnode<Attrs, State>): any {
    vnode.state.currentSortedColumnIndex = Stream(-1);
    if (!vnode.attrs.draggable) {
      return;
    }

    vnode.state.dragStart = (e: DragEvent) => {
      vnode.state.dragged           = Number((e.currentTarget as HTMLElement).dataset.id);
      vnode.state.dragging          = vnode.state.dragged;
      e.dataTransfer!.effectAllowed = "move";
      e.dataTransfer!.setData("text/html", "");
    };

    vnode.state.dragOver = (e: DragEvent) => {
      e.preventDefault();

      const toBeReplaced                 = e.currentTarget as HTMLElement;
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
    return <table class={classnames(styles.table, tableCss)}
                  data-test-id={vnode.attrs["data-test-id"] || "table"}>
      {vnode.attrs.caption ? <caption>{vnode.attrs.caption}</caption> : undefined}
      <thead data-test-id="table-header">
      <tr data-test-id="table-header-row">
        {draggableColHeader}
        {vnode.attrs.headers
              .map((header: any, index: number) => {
                return <TableHeader name={Table.renderedValue(header)}
                                    columnIndex={index}
                                    currentSortedColumnIndex={vnode.state.currentSortedColumnIndex}
                                    sortCallBackHandler={vnode.attrs.sortHandler}/>;
              })
        }
      </tr>
      </thead>
      <tbody data-test-id="table-body">
      {
        _.map(vnode.attrs.data, ((row, index) => {
          if (!vnode.attrs.draggable) {
            return (
              <tr key={index.toString()}
                  data-id={index}
                  data-test-id="table-row">
                {_.map(row, (cell) => <td>{Table.renderedValue(cell)}</td>)}
              </tr>
            );
          }
          const dragging = (Number(index) === vnode.state.dragging) ? styles.draggableOver : undefined;
          return (
            <tr key={index.toString()}
                data-id={index}
                class={dragging}
                draggable={true}
                ondragstart={vnode.state.dragStart.bind(this)}
                ondragover={vnode.state.dragOver.bind(this)}
                ondragend={vnode.state.dragEnd.bind(this)}
                data-test-id="table-row">
              <td
                data-id={index}
                onmouseover={Table.disableEvent.bind(this)}>
                <i class={styles.dragIcon} data-test-id={"table-row-drag-icon"}/>
              </td>
              {_.map(row,
                     ((cell) => <td draggable={false}
                                    ondragstart={Table.disableEvent.bind(this)}
                                    ondragend={Table.disableEvent.bind(this)}
                                    ondragover={Table.disableEvent.bind(this)}>
                       {Table.renderedValue(cell)}</td>))}
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
