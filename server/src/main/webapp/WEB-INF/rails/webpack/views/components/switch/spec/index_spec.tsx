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
import m from "mithril";
import Stream from "mithril/stream";
import {TestHelper} from "views/pages/spec/test_helper";
import {SwitchBtn} from "../index";
import styles from "../index.scss";

describe("SwitchBtn component", () => {
  const switchStream = Stream(false);

  const helper = new TestHelper();

  beforeEach(() => {
    switchStream(false);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render switch", () => {
    mount();

    expect(helper.byTestId("switch-wrapper")).toBeInDOM();
    expect(helper.byTestId("switch-wrapper")).toHaveClass(styles.switchBtn);

    expect(helper.byTestId("switch-label")).toContainText("This is switch");
    expect(helper.byTestId("switch-label")).toHaveClass(styles.switchLabel);

    expect(helper.byTestId("switch-checkbox")).toHaveClass(styles.switchInput);
    expect(helper.byTestId("switch-paddle")).toHaveClass(styles.switchPaddle);
  });

  it("should render small switch", () => {
    mount(true);

    expect(helper.byTestId("switch-wrapper")).toBeInDOM();
    expect(helper.byTestId("switch-wrapper")).toHaveClass(styles.switchBtn);
    expect(helper.byTestId("switch-wrapper")).toHaveClass(styles.switchSmall);
  });

  it("should toggle a state of field", () => {
    mount(true);

    expect(switchStream()).toBe(false);

    helper.clickByTestId("switch-paddle");
    expect(switchStream()).toBe(true);

    helper.clickByTestId("switch-paddle");
    expect(switchStream()).toBe(false);
  });

  it("should toggle a state of field on click of label", () => {
    mount(true);

    expect(switchStream()).toBe(false);

    helper.clickByTestId("switch-paddle");
    expect(switchStream()).toBe(true);

    helper.clickByTestId("switch-paddle");
    expect(switchStream()).toBe(false);
  });

  it('should render errors if any', () => {
    helper.mount(() => <SwitchBtn field={switchStream} label={"This is switch"} small={false}
                                  errorText={"Some error text"}/>);

    expect(helper.q("#switch-btn-error-text")).toBeInDOM();
    expect(helper.q("#switch-btn-error-text").textContent).toBe('Some error text');
  });

  function mount(isSmallSwitch?: boolean) {
    helper.mount(() => <SwitchBtn field={switchStream} label={"This is switch"} small={isSmallSwitch}/>);
  }

});
