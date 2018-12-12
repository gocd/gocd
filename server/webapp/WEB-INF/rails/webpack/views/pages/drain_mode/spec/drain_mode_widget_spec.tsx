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
import * as simulateEvent from "simulate-event";
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {TestData} from "views/pages/drain_mode/spec/test_data";
import * as styles from "../index.scss";

describe("Drain mode widget test", () => {
  let $root: any, root: any;
  const toggleDrainMode = jasmine.createSpy("onToggle");
  const onCancelStage   = jasmine.createSpy("onCancelStage");

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
    mount();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount() {
    m.mount(root, {
      view() {
        return (<DrainModeWidget toggleDrainMode={toggleDrainMode}
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

  xit("should callback the save function when save button is clicked", () => {
    simulateEvent.simulate(find("save-drain-mode-settings").get(0), "click");
  });

  xit("should callback the reset function when reset button is clicked", () => {
    m.redraw();

    simulateEvent.simulate(find("reset-drain-mode-settings").get(0), "click");
  });
});
