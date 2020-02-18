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
import {FillableSection} from "../fillable_section";
import css from "../fillable_section.scss";

describe("AddPipeline: FillableSection", () => {
  const sel = asSelector<typeof css>(css);
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
    const top = helper.q(sel.fillable);
    expect(top).toBeTruthy();
  });

  it("Renders child elements", () => {
    const top = helper.q(sel.fillable);
    expect(top.querySelector(".foo")).toBeTruthy();
    expect(top.querySelector(".foo")!.textContent).toBe("Some content");
  });
});
