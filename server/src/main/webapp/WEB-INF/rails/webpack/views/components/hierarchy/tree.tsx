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

import {RestyleAttrs, RestyleViewComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import {labelToId} from "views/components/forms/input_fields";
import * as defaultStyles from "./tree.scss";

type Styles = typeof defaultStyles;

interface Attrs extends RestyleAttrs<Styles> {
  datum: m.Children; // usually a single `string`, but could be a Vnode;
  "children-label"?: m.Children | (() => m.Children);
}

function asArray(children: m.ChildArrayOrPrimitive): m.ChildArray {
  return (children instanceof Array) ? children : [children];
}

// This is meant to be used recursively; by itself, it is just a node. Which,
// is of course, the same as a tree :).
export class Tree extends RestyleViewComponent<Styles, Attrs> {
  css: Styles = defaultStyles;

  view(vnode: m.Vnode<Attrs>): m.Children {
    const treeDatum    = this.datum(vnode);
    const treeChildren = this.children(vnode);
    return <dl class={this.css.tree}>
      <dt data-test-id={`tree-node-${labelToId(treeDatum)}`} class={this.css.treeDatum}>{treeDatum}</dt>
      <dd data-test-id={`tree-children-${labelToId(treeChildren)}`} class={this.css.treeChildren}>{treeChildren}</dd>
    </dl>;
  }

  datum(vnode: m.Vnode<Attrs>): m.Children {
    return vnode.attrs.datum;
  }

  children(vnode: m.Vnode<Attrs>): m.Children {
    if ("undefined" === typeof vnode.children) { return; }

    const children = asArray(vnode.children);
    if (children.length) {
      return [
        this.childLabel(vnode),
        <ul>{_.map(children, (i) => <li class={this.css.treeChild}>{i}</li>)}</ul>
      ];
    }
  }

  childLabel(vnode: m.Vnode<Attrs>): m.Children {
    const label = vnode.attrs["children-label"];
    const value = "function" === typeof label ? label() : label;
    if (!_.isNil(value)) {
      return <div class={this.css.treeChildrenLabel}>{value}</div>;
    }
  }
}
