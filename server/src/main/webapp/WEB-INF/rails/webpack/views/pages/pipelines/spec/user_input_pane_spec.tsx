/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {UserInputPane} from "../user_input_pane";
import css from "../user_input_pane.scss";

describe("AddPipeline: UserInputPane", () => {
  const sel = asSelector<typeof css>(css);
  const helper = new TestHelper();

  beforeEach(() => {
    helper.mount(() => {
      return <UserInputPane heading="Episode IV: A New Hope">
        <p class="nerd-alert">A long time ago in a galaxy far, far away...</p>
      </UserInputPane>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure with heading", () => {
    const top = helper.q(sel.userInput);
    expect(top).toBeTruthy();

    expect(helper.q(sel.sectionHeading, top)).toBeTruthy();
    expect(helper.text(sel.sectionHeading, top)).toBe("Episode IV: A New Hope");

    expect(helper.q(sel.sectionNote, top)).toBeTruthy();
    expect(helper.text(sel.sectionNote, top)).toBe("* denotes a required field");
  });

  it("Renders child elements", () => {
    const top = helper.q(sel.userInput);
    expect(helper.q("p.nerd-alert", top)).toBeTruthy();
    expect(helper.text("p.nerd-alert", top)).toBe("A long time ago in a galaxy far, far away...");
  });
});
