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

import asSelector from "helpers/selector_proxy";
import * as m from "mithril";
import * as events from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {AdvancedSettings} from "../advanced_settings";
import * as css from "../components.scss";

describe("AddPipeline: AdvancedSettings", () => {
  const sel = asSelector<typeof css>(css);
  const helper = new TestHelper();

  beforeEach(() => {
    helper.mount(() => {
      return <AdvancedSettings>
        <span class="foo">Some content</span>
      </AdvancedSettings>;
    });
  });

  afterEach(helper.unmount.bind(helper));

  it("Generates element hierarchy and child elements", () => {
    const top = helper.q(sel.advancedSettings);
    expect(top).toBeTruthy();
    expect(helper.q(sel.summary, top)).toBeTruthy();
    expect(helper.text(sel.summary, top)).toBe("Advanced Settings");

    expect(helper.q(sel.details, top)).toBeTruthy();
    expect(helper.q(`${sel.details} .foo`, top)).toBeTruthy();
    expect(helper.text(`${sel.details} .foo`, top)).toBe("Some content");
  });

  it("Clicking toggles the open class", () => {
    const top = helper.q(sel.advancedSettings);
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("none");

    events.simulate(helper.q(sel.summary, top), "click");
    expect(top.classList.contains(css.open)).toBe(true);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("block");

    events.simulate(helper.q(sel.summary, top), "click");
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("none");
  });
});
