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
import {MaintenanceModeInfo} from "models/maintenance_mode/types";
import {DisabledSubsystemsWidget} from "views/pages/maintenance_mode/disabled_susbsystems_widget";
import {TestData} from "views/pages/maintenance_mode/spec/test_data";

describe("Maintenance Mode Disabled subsystem Widget", () => {
  let $root: any, root: any;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  describe("During Maintenance Mode", () => {
    describe("Has No Running Services", () => {
      beforeEach(() => {
        mount(TestData.noRunningSystemsInfo());
      });

      it("should show mdu stopped information", () => {
        const expectedMessage = "Stopped material subsystem.";
        expect(find("mdu-stopped")).toContainText(expectedMessage);
        expect(find("mdu-stopped")).toContainElement(checkIcon());
        expect(find("mdu-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show config repo stopped information", () => {
        const expectedMessage = "Stopped polling on config repositories.";
        expect(find("config-repo-polling-stopped")).toContainText(expectedMessage);
        expect(find("config-repo-polling-stopped")).toContainElement(checkIcon());
        expect(find("config-repo-polling-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show jobs building stopped information", () => {
        const expectedMessage = "Stopped scheduling subsystem.";
        expect(find("scheduling-system-stopped")).toContainText(expectedMessage);
        expect(find("scheduling-system-stopped")).toContainElement(checkIcon());
        expect(find("scheduling-system-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show agent assignment stopped information", () => {
        const expectedMessage = "Stopped assigning jobs to agents.";
        expect(find("agent-subsystem-stopped")).toContainText(expectedMessage);
        expect(find("agent-subsystem-stopped")).toContainElement(checkIcon());
        expect(find("agent-subsystem-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show manual pipeline trigger stopped information", () => {
        const expectedMessage = "Stopped pipeline triggers.";
        expect(find("manual-trigger-stopped")).toContainText(expectedMessage);
        expect(find("manual-trigger-stopped")).toContainElement(checkIcon());
        expect(find("manual-trigger-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show config modification stopped information", () => {
        const expectedMessage = "Stopped config modifications.";
        expect(find("config-changes-stopped")).toContainText(expectedMessage);
        expect(find("config-changes-stopped")).toContainElement(checkIcon());
        expect(find("config-changes-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show db modification stopped information", () => {
        const expectedMessage = "Stopped database and filesystem modifications.";
        expect(find("db-changes-stopped")).toContainText(expectedMessage);
        expect(find("db-changes-stopped")).toContainElement(checkIcon());
        expect(find("db-changes-stopped")).not.toContainElement(spinnerIcon());
      });
    });

    describe("Has running subsystems", () => {
      beforeEach(() => {
        mount(TestData.info(true));
      });

      it("should show mdu stopped information", () => {
        const expectedMessage = "Waiting for material subsystem to stop..";
        expect(find("mdu-in-progress")).toContainText(expectedMessage);
        expect(find("mdu-in-progress")).toContainElement(spinnerIcon());
        expect(find("mdu-in-progress")).not.toContainElement(checkIcon());
      });

      it("should show config repo stopped information", () => {
        const expectedMessage = "Stopped polling on config repositories.";
        expect(find("config-repo-polling-stopped")).toContainText(expectedMessage);
        expect(find("config-repo-polling-stopped")).toContainElement(checkIcon());
        expect(find("config-repo-polling-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show jobs building in progress information", () => {
        const expectedMessage = "Waiting for building jobs to finish..";
        expect(find("scheduling-system-in-progress")).toContainText(expectedMessage);
        expect(find("scheduling-system-in-progress")).toContainElement(spinnerIcon());
        expect(find("scheduling-system-in-progress")).not.toContainElement(checkIcon());
      });

      it("should show agent assignment stopped information", () => {
        const expectedMessage = "Stopped assigning jobs to agents.";
        expect(find("agent-subsystem-stopped")).toContainText(expectedMessage);
        expect(find("agent-subsystem-stopped")).toContainElement(checkIcon());
        expect(find("agent-subsystem-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show manual pipeline trigger stopped information", () => {
        const expectedMessage = "Stopped pipeline triggers.";
        expect(find("manual-trigger-stopped")).toContainText(expectedMessage);
        expect(find("manual-trigger-stopped")).toContainElement(checkIcon());
        expect(find("manual-trigger-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show config modification stopped information", () => {
        const expectedMessage = "Stopped config modifications.";
        expect(find("config-changes-stopped")).toContainText(expectedMessage);
        expect(find("config-changes-stopped")).toContainElement(checkIcon());
        expect(find("config-changes-stopped")).not.toContainElement(spinnerIcon());
      });

      it("should show db modification stopped information", () => {
        const expectedMessage = "Stopped database and filesystem modifications.";
        expect(find("db-changes-stopped")).toContainText(expectedMessage);
        expect(find("db-changes-stopped")).toContainElement(checkIcon());
        expect(find("db-changes-stopped")).not.toContainElement(spinnerIcon());
      });
    });
  });

  describe("Not During Maintenance Mode", () => {
    beforeEach(() => {
      mount(TestData.info(false));
    });

    it("should provide header information of what will be disabled during maintenance mode", () => {
      const expectedHeader = "Enabling GoCD Server Maintenance mode will:";
      expect(find("info-when-not-in-maintenance-mode-header")).toContainText(expectedHeader);
    });

    it("should provide the list of all the systems that will be disabled", () => {
      const stopMaterials       = "Stop the material subsystem so that no new materials are polled.";
      const stopConfigRepos     = "Stop polling on config repositories.";
      const stopPipelineTrigger = "Stop the scheduling subsystem so that no new pipelines are triggered (automatically or through timers).";
      const stopAgentAssignment = "Stop the agent subsystem, so that no agents can pick up work if they’re idle.";
      const stopManualTrigger   = "Prevent users from triggering pipelines.";
      const stopConfigChanges   = "Prevent users from modifying configurations.";
      const stopDBChanges       = "Prevent users from performing operations that modifies state in the database or filesystem.";

      expect(find("stop-material").get(0)).toContainText(stopMaterials);
      expect(find("stop-config-repo").get(0)).toContainText(stopConfigRepos);
      expect(find("stop-pipeline-scheduling").get(0)).toContainText(stopPipelineTrigger);
      expect(find("stop-work-assignment").get(0)).toContainText(stopAgentAssignment);
      expect(find("stop-manual-trigger").get(0)).toContainText(stopManualTrigger);
      expect(find("stop-config-changes").get(0)).toContainText(stopConfigChanges);
      expect(find("stop-db-changes").get(0)).toContainText(stopDBChanges);
    });
  });

  //private
  function mount(maintenanceModeInfo: MaintenanceModeInfo) {
    m.mount(root, {
      view() {
        return (<DisabledSubsystemsWidget maintenanceModeInfo={maintenanceModeInfo}/>);
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

  function checkIcon() {
    return `[data-test-id='Check-icon']`;
  }

  function spinnerIcon() {
    return `[data-test-id='Spinner-icon']`;
  }
});
