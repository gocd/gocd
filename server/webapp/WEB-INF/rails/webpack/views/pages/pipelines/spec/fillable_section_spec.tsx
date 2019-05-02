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

import * as m from "mithril";
import {TestHelper} from "views/pages/spec/test_helper";
import * as css from "../components.scss";
import {FillableSection} from "../fillable_section";

describe("AddPipeline: FillableSection", () => {
  const helper = new TestHelper();

  beforeEach(() => {
    helper.mount(() => {
      return <FillableSection>
        <span class="foo">Some content</span>
      </FillableSection>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates structure", () => {
    const top = helper.find(`.${css.fillable}`)[0];
    expect(top).toBeTruthy();
  });

  it("Renders child elements", () => {
    const top = helper.find(`.${css.fillable}`)[0];
    expect(top.querySelector(".foo")).toBeTruthy();
    expect(top.querySelector(".foo")!.textContent).toBe("Some content");
  });
});
