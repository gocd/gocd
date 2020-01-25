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

import {asSelector} from "helpers/css_proxies";
import m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import {CollapsibleTree, Tree} from "../tree";
import css from "../tree.scss";

describe("CollapsibleTree", () => {
  const sel    = asSelector<typeof css>(css);
  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("renders single node", () => {
    helper.mount(() => <CollapsibleTree datum="datum" dataTestId={"root"}/>);

    const tree = helper.byTestId("root");

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeDatum, tree)).toBeInDOM();
    expect(helper.text(sel.treeDatum, tree)).toBe("datum");
    expect(helper.q(sel.treeChildren, tree)).not.toBeInDOM();
    expect(helper.q(sel.treeChild, tree)).not.toBeInDOM();
  });

  it("should not render icon if has no children", () => {
    helper.mount(() => <CollapsibleTree datum="datum" dataTestId={"root"}/>);

    const tree = helper.byTestId("root");

    expect(helper.q("i", tree)).not.toBeInDOM();
  });

  it("should not render icon if has at least one child", () => {
    helper.mount(() => <CollapsibleTree datum="root" dataTestId={"root"}>
      <CollapsibleTree datum="ChildOne" dataTestId={"first-child"}/>
    </CollapsibleTree>);

    const tree  = helper.byTestId("root");
    const child = helper.byTestId("first-child");

    expect(helper.q("i", tree)).toBeInDOM();
    expect(helper.q("i", child)).not.toBeInDOM();
  });

  it("should toggle children visibility on click of expand collapse icon", () => {
    helper.mount(() => <CollapsibleTree datum="root" dataTestId={"root"}>
      <CollapsibleTree datum="ChildOne" dataTestId={"first-child"}/>
    </CollapsibleTree>);

    const caretDownIcon = helper.byTestId("root-icon");
    expect(helper.byTestId("first-child")).toBeInDOM();
    expect(caretDownIcon).toHaveAttr("title", "Caret Down");

    helper.click(caretDownIcon);

    const caretRightIcon = helper.byTestId("root-icon");
    expect(caretDownIcon).not.toBeInDOM();
    expect(helper.byTestId("first-child")).not.toBeInDOM();
    expect(caretRightIcon).toHaveAttr("title", "Caret Right");

    helper.click(caretRightIcon);

    expect(helper.byTestId("first-child")).toBeInDOM();
    expect(caretRightIcon).not.toBeInDOM();
    expect(helper.byTestId("root-icon")).toBeInDOM();
  });

  it("renders optional children label if provided", () => {
    helper.mount(() => <CollapsibleTree datum="datum" children-label="for the children!">yay</CollapsibleTree>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeChildrenLabel, tree)).toBeInDOM();
    expect(helper.text(sel.treeChildrenLabel, tree)).toBe("for the children!");
    expect(helper.textAll(sel.treeChild, tree)).toEqual(["yay"]);
  });

  it("does not render children label when there are no children", () => {
    helper.mount(() => <CollapsibleTree datum="datum" children-label="for the children!"/>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeChildrenLabel, tree)).not.toBeInDOM();
  });

  it("nests children", () => {
    helper.mount(() => <CollapsibleTree datum="datum" children-label="for the children!">
      <Tree datum="lil one">even lil'er</Tree>
    </CollapsibleTree>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.text(sel.treeDatum, tree)).toBe("datum");
    expect(helper.text(sel.treeChildrenLabel, tree)).toBe("for the children!");

    const child = helper.q(sel.tree, tree);

    expect(child).toBeInDOM();
    expect(helper.text(sel.treeDatum, child)).toBe("lil one");
    expect(helper.q(sel.treeChildrenLabel, child)).not.toBeInDOM();
    expect(helper.text(sel.treeChild, child)).toBe("even lil'er");
  });
});
