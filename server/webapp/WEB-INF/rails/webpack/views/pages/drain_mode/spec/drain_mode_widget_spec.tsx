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
import {DrainModeWidget} from "views/pages/drain_mode/drain_mode_widget";
import {TestData} from "views/pages/drain_mode/spec/test_data";

describe("Drain Mode Widget", () => {
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

  it("should provide the description of the drain mode feature", () => {
    const expectedDescription = "The drain mode is a maintenance mode which a GoCD system administrator can put GoCD into so that it is safe to restart it or upgrade it without having running jobs reschedule when it is back.";
    expect(find("drain-mode-description")).toContainText(expectedDescription);
  });

  it("should provide the drain mode updated information", () => {
    const expectedUpdatedByInfo = `${TestData.info().metdata.updatedBy} changed the drain mode state on ${TestData.info().metdata.updatedOn}.`;
    expect(find("drain-mode-updated-by-info")).toContainText(expectedUpdatedByInfo);
  });

  //private
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
});
