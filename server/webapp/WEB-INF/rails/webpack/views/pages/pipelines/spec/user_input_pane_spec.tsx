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
import * as m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import * as styles from "../components.scss";
import {UserInputPane} from "../user_input_pane";

describe("AddPipeline: UserInputPane", () => {
  const helper = new TestHelper();
  const cls = bind(styles);

  beforeEach(() => {
    helper.mount(() => {
      return <UserInputPane heading="Episode IV: A New Hope">
        <p class="nerd-alert">A long time ago in a galaxy far, far away...</p>
      </UserInputPane>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure with heading", () => {
    const top = helper.find(`.${cls(styles.userInput)}`)[0];
    expect(top).toBeTruthy();

    const heading = top!.querySelector(`.${cls(styles.sectionHeading)}`);
    expect(heading).not.toBeNull();
    expect(heading!.textContent).toBe("Episode IV: A New Hope");

    const note = top.querySelector(`.${cls(styles.sectionNote)}`);
    expect(note).not.toBeNull();
    expect(note!.textContent).toBe("* denotes a required field");
  });

  it("Renders child elements", () => {
    const top = helper.find(`.${cls(styles.userInput)}`)[0];
    expect(top.querySelector("p.nerd-alert")).toBeTruthy();
    expect(top.querySelector("p.nerd-alert")!.textContent).toBe("A long time ago in a galaxy far, far away...");
  });
});
