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
import {MaintenanceModeWidget} from "views/pages/maintenance_mode/maintenance_mode_widget";
import {TestData} from "views/pages/maintenance_mode/spec/test_data";

describe("Maintenance Mode Widget", () => {
  let $root: any, root: any;
  const toggleMaintenanceMode = jasmine.createSpy("onToggle");
  const onCancelStage         = jasmine.createSpy("onCancelStage");

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
    mount();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should provide the description of the maintenance mode feature", () => {
    const expectedDescription = "When put into maintenance mode, it is safe to restart or upgrade the GoCD server without having any running jobs reschedule when the server is back up.";
    expect(find("maintenance-mode-description")).toContainText(expectedDescription);
  });

  it("should add a link to the maintenance mode documentation", () => {
    const expectedLink = "https://docs.gocd.org/current/advanced_usage/maintenance_mode.html";
    const expectedText = "Learn more..";

    expect($root.find("a")[0].href).toBe(expectedLink);
    expect($root.find("a")[0].innerText).toBe(expectedText);
  });

  it("should provide the maintenance mode updated information", () => {
    const expectedUpdatedByInfo = `${TestData.info().metdata.updatedBy} changed the maintenance mode state on ${TestData.info().metdata.updatedOn}.`;
    expect(find("maintenance-mode-updated-by-info")).toContainText(expectedUpdatedByInfo);
  });

  //private
  function mount() {
    m.mount(root, {
      view() {
        return (<MaintenanceModeWidget toggleMaintenanceMode={toggleMaintenanceMode}
                                       onCancelStage={onCancelStage}
                                       maintenanceModeInfo={TestData.info()}/>);
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
