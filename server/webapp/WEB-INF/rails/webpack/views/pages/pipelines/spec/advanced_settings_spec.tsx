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
import * as events from "simulate-event";
import {TestHelper} from "views/pages/spec/test_helper";
import {AdvancedSettings} from "../advanced_settings";
import * as css from "../components.scss";

describe("AddPipeline: AdvancedSettings", () => {
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
    const top = helper.find(`.${css.advancedSettings}`)[0];
    expect(top).toBeTruthy();
    expect(top.querySelector(`.${css.summary}`)).toBeTruthy();
    expect(top.querySelector(`.${css.summary}`)!.textContent).toBe("Advanced Settings");

    expect(top.querySelector(`.${css.details}`)).toBeTruthy();
    expect(top.querySelector(`.${css.details} .foo`)).toBeTruthy();
    expect(top.querySelector(`.${css.details} .foo`)!.textContent).toBe("Some content");
  });

  it("Clicking toggles the open class", () => {
    const top = helper.find(`.${css.advancedSettings}`)[0];
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(top.querySelector(`.${css.details}`)!).display).toBe("none");

    events.simulate(top.querySelector(`.${css.summary}`)!, "click");
    expect(top.classList.contains(css.open)).toBe(true);
    expect(window.getComputedStyle(top.querySelector(`.${css.details}`)!).display).toBe("block");

    events.simulate(top.querySelector(`.${css.summary}`)!, "click");
    expect(top.classList.contains(css.open)).toBe(false);
    expect(window.getComputedStyle(top.querySelector(`.${css.details}`)!).display).toBe("none");
  });
});
