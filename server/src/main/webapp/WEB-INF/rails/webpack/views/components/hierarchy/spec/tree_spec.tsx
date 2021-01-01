/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {Tree} from "../tree";
import css from "../tree.scss";

describe("<Tree/>", () => {
  const sel = asSelector<typeof css>(css);
  const helper = new TestHelper();

  afterEach(() => helper.unmount());

  it("renders single node", () => {
    helper.mount(() => <Tree datum="datum"/>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeDatum, tree)).toBeInDOM();
    expect(helper.text(sel.treeDatum, tree)).toBe("datum");
    expect(helper.q(sel.treeChildren, tree)).toBeInDOM();
    expect(helper.q(sel.treeChild, tree)).not.toBeInDOM();
  });

  it("renders optional children label if provided", () => {
    helper.mount(() => <Tree datum="datum" children-label="for the children!">yay</Tree>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeChildrenLabel, tree)).toBeInDOM();
    expect(helper.text(sel.treeChildrenLabel, tree)).toBe("for the children!");
    expect(helper.textAll(sel.treeChild, tree)).toEqual(["yay"]);
  });

  it("does not render children label when there are no children", () => {
    helper.mount(() => <Tree datum="datum" children-label="for the children!"/>);

    const tree = helper.q(sel.tree);

    expect(tree).toBeInDOM();
    expect(helper.q(sel.treeChildrenLabel, tree)).not.toBeInDOM();
  });

  it("nests children", () => {
    helper.mount(() => <Tree datum="datum" children-label="for the children!">
      <Tree datum="lil one">even lil'er</Tree>
    </Tree>);

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
