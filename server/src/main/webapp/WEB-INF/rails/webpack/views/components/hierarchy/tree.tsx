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
import {RestyleAttrs, RestyleViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {labelToId} from "views/components/forms/input_fields";
import {CaretDown, CaretRight} from "views/components/icons";
import styles from "views/components/site_menu/index.scss";
import * as defaultStyles from "./tree.scss";

type Styles = typeof defaultStyles;
const classnames = bind(styles);

interface Attrs extends RestyleAttrs<Styles> {
  datum: m.Children; // usually a single `string`, but could be a Vnode;
  "children-label"?: m.Children | (() => m.Children);
}

function asArray(children: m.ChildArrayOrPrimitive): m.ChildArray {
  return (children instanceof Array) ? children : [children];
}

// This is meant to be used recursively; by itself, it is just a node. Which,
// is of course, the same as a tree :).
export class Tree<V = {}> extends RestyleViewComponent<Styles, Attrs & V> {
  css: Styles = defaultStyles;

  view(vnode: m.Vnode<Attrs & V>): m.Children {
    const treeDatum    = this.datum(vnode);
    const treeChildren = this.children(vnode);
    return <dl class={this.css.tree}>
      <dt data-test-id={`tree-node-${labelToId(treeDatum)}`} class={this.css.treeDatum}>{treeDatum}</dt>
      <dd data-test-id={`tree-children-${labelToId(treeChildren)}`} class={this.css.treeChildren}>{treeChildren}</dd>
    </dl>;
  }

  datum(vnode: m.Vnode<Attrs & V>): m.Children {
    return vnode.attrs.datum;
  }

  children(vnode: m.Vnode<Attrs & V>): m.Children {
    if ("undefined" === typeof vnode.children) {
      return;
    }

    const children = asArray(vnode.children);
    if (children.length) {
      return [
        this.childLabel(vnode),
        <ul>{_.map(children, (i) => <li class={this.css.treeChild}>{i}</li>)}</ul>
      ];
    }
  }

  childLabel(vnode: m.Vnode<Attrs & V>): m.Children {
    const label = vnode.attrs["children-label"];
    const value = "function" === typeof label ? label() : label;
    if (!_.isNil(value)) {
      return <div class={this.css.treeChildrenLabel}>{value}</div>;
    }
  }
}

interface CollapsibleAttrs extends Attrs {
  collapsed?: boolean;
  onclick?: () => void;
  selected?: boolean;
  dataTestId?: string;
}

export class CollapsibleTree extends Tree<CollapsibleAttrs> {
  readonly hideChildren = Stream<boolean>();

  oninit(vnode: m.Vnode<CollapsibleAttrs, {}>) {
    super.oninit(vnode);
    this.hideChildren(vnode.attrs.collapsed || false);
  }

  view(vnode: m.Vnode<CollapsibleAttrs>): m.Children {
    const treeDatum    = this.datum(vnode);
    const treeChildren = this.children(vnode);
    const icon         = this.icon(vnode, treeChildren);
    let maybeChildren  = null;

    if (!this.hideChildren() && treeChildren) {
      maybeChildren = <dd data-test-id={`tree-children-${labelToId(treeChildren)}`} class={this.css.treeChildren}>
        {treeChildren}
      </dd>;
    }

    return <dl class={this.css.tree} data-test-id={vnode.attrs.dataTestId}>
      <dt data-test-id={`tree-node-${labelToId(treeDatum)}`} class={classnames(this.css.treeDatum)}>
        {icon}
        <span class={classnames({[this.css.selected]: vnode.attrs.selected})}
              onclick={this.ondatumClick.bind(this, vnode)}>{treeDatum}</span>
      </dt>
      {maybeChildren}
    </dl>;
  }

  toggle() {
    this.hideChildren(!this.hideChildren());
  }

  ondatumClick(vnode: m.Vnode<CollapsibleAttrs>) {
    if (vnode.attrs.onclick) {
      vnode.attrs.onclick();
    }
  }

  icon(vnode: m.Vnode<CollapsibleAttrs>, children: m.Children) {
    if (!children) {
      return;
    }

    if (this.hideChildren()) {
      return <CaretRight iconOnly={true} data-test-id={`${vnode.attrs.dataTestId}-icon`}
                         onclick={this.toggle.bind(this)}/>;
    }
    return <CaretDown iconOnly={true} data-test-id={`${vnode.attrs.dataTestId}-icon`}
                      onclick={this.toggle.bind(this)}/>;
  }
}
