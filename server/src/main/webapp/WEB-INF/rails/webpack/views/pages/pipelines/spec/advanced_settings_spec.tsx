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
import {AdvancedSettings} from "../advanced_settings";
import css from "../advanced_settings.scss";

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
    expect(top.classList.contains(css.lockOpen)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("none");

    helper.click(sel.summary, top);
    expect(top.classList.contains(css.open)).toBe(true);
    expect(top.classList.contains(css.lockOpen)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("block");

    helper.click(sel.summary, top);
    expect(top.classList.contains(css.open)).toBe(false);
    expect(top.classList.contains(css.lockOpen)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("none");
  });

  it("Can be locked in an open state with forceOpen attribute", () => {
    helper.unmount();
    helper.mount(() => {
      return <AdvancedSettings forceOpen={true}>
        <span class="foo">Some content</span>
      </AdvancedSettings>;
    });

    const top = helper.q(sel.advancedSettings);

    expect(top.classList.contains(css.lockOpen)).toBe(true);
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("block");

    helper.click(sel.summary, top); // click has no effect
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(helper.q(sel.details, top)).display).toBe("block");
  });
});
