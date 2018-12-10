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

import * as m from "mithril";
import {DrainModeSettings} from "models/drain_mode/drain_mode_settings";
import * as simulateEvent from "simulate-event";
import {MessageType} from "views/components/flash_message";
import {Message} from "views/pages/drain_mode";
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {TestData} from "views/pages/drain_mode/spec/test_data";
import * as styles from "../index.scss";

describe("Drain mode widget test", () => {
  let $root: any, root: any;
  const onSave            = jasmine.createSpy("onSave");
  const onReset           = jasmine.createSpy("onReset");
  const onCancelStage     = jasmine.createSpy("onCancelStage");
  const drainModeSettings = new DrainModeSettings(true, "bob", "2018-12-04T06:35:56Z");

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
    mount();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount(message?: Message) {
    m.mount(root, {
      view() {
        return (<DrainModeWidget settings={drainModeSettings}
                                 onSave={onSave}
                                 onReset={onReset}
                                 onCancelStage={onCancelStage}
                                 drainModeInfo={TestData.info()}/>);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  it("should show drain mode information", () => {
    expect(find("drain-mode-widget")).toBeInDOM();
    expect(find("switch-wrapper")).toBeInDOM();
    expect($root.find(`.${styles.drainModeInfo}`)).toContainText("Is server in drain mode: true");
    expect($root.find(`.${styles.drainModeInfo}`)).toContainText("Drain mode updated by: bob");
    expect($root.find(`.${styles.drainModeInfo}`)).toContainText("Drain mode updated on: 04 Dec 2018");
  });

  it("should render message when one present", () => {
    mount(new Message(MessageType.alert, "Foo"));

    expect(find("flash-message-alert")).toContainText("Foo");
  });

  it("should not render message when not present", () => {
    expect(find("flash-message-alert")).not.toBeInDOM();
  });

  it("should callback the save function when save button is clicked", () => {
    simulateEvent.simulate(find("save-drain-mode-settings").get(0), "click");

    expect(onSave).toHaveBeenCalledWith(drainModeSettings, jasmine.any(Event));
  });

  it("should callback the reset function when reset button is clicked", () => {
    m.redraw();

    simulateEvent.simulate(find("reset-drain-mode-settings").get(0), "click");

    expect(onReset).toHaveBeenCalledWith(drainModeSettings, jasmine.any(Event));
  });
});
