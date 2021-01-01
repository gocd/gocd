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
import {docsUrl} from "gen/gocd_version";
import {timeFormatter} from "helpers/time_formatter";
import m from "mithril";
import {MaintenanceModeWidget} from "views/pages/maintenance_mode/maintenance_mode_widget";
import {TestData} from "views/pages/maintenance_mode/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {MaintenanceModeInfo} from "../../../../models/maintenance_mode/types";

describe("Maintenance Mode Widget", () => {
  const toggleMaintenanceMode = jasmine.createSpy("onToggle");
  const onCancelStage = jasmine.createSpy("onCancelStage");
  const helper = new TestHelper();

  let info: MaintenanceModeInfo;
  beforeEach(() => {
    info = TestData.info();
    helper.mount(() => <MaintenanceModeWidget toggleMaintenanceMode={toggleMaintenanceMode}
                                              onCancelStage={onCancelStage}
                                              maintenanceModeInfo={info}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("should provide the description of the maintenance mode feature", () => {
    const expectedDescription = "When put into maintenance mode, it is safe to restart or upgrade the GoCD server without having any running jobs reschedule when the server is back up.";
    expect(helper.textByTestId("maintenance-mode-description")).toContain(expectedDescription);
  });

  it("should add a link to the maintenance mode documentation", () => {
    const expectedLink = docsUrl("/advanced_usage/maintenance_mode.html");
    const expectedText = "Learn more..";

    expect((helper.q("a") as HTMLAnchorElement).href).toBe(expectedLink);
    expect(helper.q("a").innerText).toBe(expectedText);
  });

  it("should provide the maintenance mode updated information", () => {
    const updatedOn = timeFormatter.format(TestData.UPDATED_ON);
    const expectedUpdatedByInfo = `${TestData.info().metdata.updatedBy} changed the maintenance mode state on ${updatedOn}.`;
    expect(helper.textByTestId("maintenance-mode-updated-by-info")).toContain(expectedUpdatedByInfo);
  });

  it("should provide the maintenance mode updated information when GoCD Server is started in maintenance mode", () => {
    info.metdata.updatedBy = "GoCD";
    helper.redraw();

    const updatedOn = timeFormatter.format(TestData.UPDATED_ON);
    const expectedUpdatedByInfo = `GoCD Server is started in maintenance mode at ${updatedOn}.`;
    expect(helper.textByTestId("maintenance-mode-updated-by-info")).toContain(expectedUpdatedByInfo);
  });
});
