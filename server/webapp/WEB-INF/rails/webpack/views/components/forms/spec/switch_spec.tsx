/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as stream from "mithril/stream";
import {Switch} from "views/components/forms/input_fields";
import * as styles from "../forms.scss";

describe("Switch component", () => {
  const m             = require("mithril");
  const simulateEvent = require("simulate-event");
  const switchStream  = stream(false);

  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
    switchStream(false);
  });

  afterEach(unmount);

  it("should render switch", () => {
    mount();

    expect(find("switch-wrapper")).toBeInDOM();
    expect(find("switch-wrapper")).toHaveClass(styles.switchBtn);

    expect(find("form-field-label")).toContainText("This is switch");

    expect(find("form-field-input")).toHaveClass(styles.switchInput);
    expect(find("switch-paddle")).toHaveClass(styles.switchPaddle);
  });

  it("should render small switch", () => {
    mount(true);

    expect(find("switch-wrapper")).toBeInDOM();
    expect(find("switch-wrapper")).toHaveClass(styles.switchBtn);
    expect(find("switch-wrapper")).toHaveClass(styles.switchSmall);
  });

  it("should toggle a state of field", () => {
    mount(true);

    expect(switchStream()).toBe(false);

    simulateEvent.simulate(find("form-field-input").get(0), "click");
    expect(switchStream()).toBe(true);

    simulateEvent.simulate(find("form-field-input").get(0), "click");
    expect(switchStream()).toBe(false);
  });

  it("should toggle a state of field on click of label", () => {
    mount(true);

    expect(switchStream()).toBe(false);

    simulateEvent.simulate(find("switch-paddle").get(0), "click");
    expect(switchStream()).toBe(true);

    simulateEvent.simulate(find("switch-paddle").get(0), "click");
    expect(switchStream()).toBe(false);
  });

  function mount(isSmallSwitch?: boolean) {
    m.mount(root, {
      view() {
        return (<Switch property={switchStream} label={"This is switch"} small={isSmallSwitch}/>);
      }
    });

    m.redraw(true);
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
