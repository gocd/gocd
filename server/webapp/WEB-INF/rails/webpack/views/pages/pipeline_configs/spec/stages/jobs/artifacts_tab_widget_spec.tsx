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

import m from "mithril";
import Stream from "mithril/stream";
import {ArtifactType, BuildArtifact, TestArtifact} from "models/new_pipeline_configs/artifact";
import {Artifacts} from "models/new_pipeline_configs/artifacts";
import * as simulateEvent from "simulate-event";
import {ArtifactsTab} from "views/pages/pipeline_configs/stages/jobs/artifacts_tab_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Pipeline Config - Artifacts Tab", () => {
  const helper = new TestHelper();

  let buildArtifact: BuildArtifact;
  let testArtifact: TestArtifact;
  let artifacts: Artifacts;

  beforeEach(() => {
    buildArtifact = new BuildArtifact("foo.txt", "src/foo.txt");
    testArtifact  = new TestArtifact("bar.txt", "src/bar.txt");
    artifacts     = new Artifacts(buildArtifact, testArtifact);
    helper.mount(() => {
      return <ArtifactsTab artifacts={Stream(artifacts)}/>;
    });
  });

  afterEach(() => {
    helper.unmount();
  });

  it("should render artifacts tab", () => {
    expect(helper.findByDataTestId("artifacts-tab")).toBeInDOM();
  });

  it("should render artifacts heading", () => {
    expect(helper.findByDataTestId("artifacts-heading")).toContainText("Artifacts");
  });

  it("should render artifacts help text", () => {
    const helpText = "Artifacts are the files created during a job run, by one of the tasks. Publish your artifacts so that it can be used by downstream pipelines.";
    expect(helper.find("#help-artifacts")).toContainText(helpText);
  });

  it("should render doc link for artifact help text", () => {
    const docLink      = helper.find("#help-artifacts a")[0];
    const expectedLink = "/configuration/dev_upload_test_report.html";
    expect((docLink as HTMLAnchorElement).getAttribute("href")).toContain(expectedLink);
  });

  describe("Built-in Artifact", () => {
    it("should render build artifact", () => {
      const buildArtifacts = helper.findByDataTestId("Build-artifact");
      expect(buildArtifacts).toHaveLength(1);
    });

    it("should render test artifact", () => {
      const testArtifacts = helper.findByDataTestId("Test-artifact");
      expect(testArtifacts).toHaveLength(1);
    });

    it("should render headings for built in artifact", () => {
      const headingContainers = helper.findByDataTestId("artifact-item-headings");
      expect(headingContainers).toHaveLength(2);

      expect(headingContainers).toContainText("Type");
      expect(headingContainers).toContainText("Source");
      expect(headingContainers).toContainText("Destination");
    });

    it("should render help text for type", () => {
      const helpText = "There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact.";
      expect(helper.find("#help-artifact-type")).toContainText(helpText);
    });

    it("should render help text for source", () => {
      const helpText = "The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).";
      expect(helper.find("#help-artifact-source")).toContainText(helpText);
    });

    it("should render help text for destination", () => {
      const helpText = "The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory";
      expect(helper.find("#help-artifact-destination")).toContainText(helpText);
    });

    describe("Build", () => {
      it("should render type of build artifact", () => {
        const artifactType = helper.findSelectorIn(helper.findByDataTestId("Build-artifact-content"), "span");
        expect(artifactType).toContainText("Build Artifact");
      });

      it("should render input field for build artifact source", () => {
        const inputWrapper             = helper.findByDataTestId("Build-artifact-source-input-wrapper");
        const buildArtifactSourceInput = helper.findSelectorIn(inputWrapper, "input");
        expect(buildArtifactSourceInput).toBeInDOM();
      });

      it("should bind input field with model source field", () => {
        const inputWrapper = helper.findByDataTestId("Build-artifact-source-input-wrapper");
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(buildArtifact.source()!);

        const newValue = "new-file.txt";
        buildArtifact.source(newValue);
        m.redraw.sync();

        expect(buildArtifact.source()).toEqual(newValue);
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(newValue);
      });

      it("should render input field for build artifact destination", () => {
        const inputWrapper                  = helper.findByDataTestId("Build-artifact-destination-input-wrapper");
        const buildArtifactDestinationInput = helper.findSelectorIn(inputWrapper, "input");
        expect(buildArtifactDestinationInput).toBeInDOM();

        expect(buildArtifactDestinationInput).toHaveValue(buildArtifact.destination()!);
      });

      it("should bind input field with model destination field", () => {
        const inputWrapper = helper.findByDataTestId("Build-artifact-destination-input-wrapper");
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(buildArtifact.destination()!);

        const newValue = "new-file.txt";
        buildArtifact.destination(newValue);
        m.redraw.sync();

        expect(buildArtifact.destination()).toEqual(newValue);
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(newValue);
      });
    });

    describe("Test", () => {
      it("should render type of test artifact", () => {
        const artifactType = helper.findSelectorIn(helper.findByDataTestId("Test-artifact-content"), "span");
        expect(artifactType).toContainText("Test Artifact");
      });

      it("should render input field for test artifact source", () => {
        const inputWrapper            = helper.findByDataTestId("Test-artifact-source-input-wrapper");
        const testArtifactSourceInput = helper.findSelectorIn(inputWrapper, "input");
        expect(testArtifactSourceInput).toBeInDOM();
      });

      it("should bind input field with model source field", () => {
        const inputWrapper = helper.findByDataTestId("Test-artifact-source-input-wrapper");
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(testArtifact.source()!);

        const newValue = "new-file.txt";
        testArtifact.source(newValue);
        m.redraw.sync();

        expect(testArtifact.source()).toEqual(newValue);
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(newValue);
      });

      it("should render input field for test artifact destination", () => {
        const inputWrapper                 = helper.findByDataTestId("Test-artifact-destination-input-wrapper");
        const testArtifactDestinationInput = helper.findSelectorIn(inputWrapper, "input");
        expect(testArtifactDestinationInput).toBeInDOM();

        expect(testArtifactDestinationInput).toHaveValue(testArtifact.destination()!);
      });

      it("should bind input field with model destination field", () => {
        const inputWrapper = helper.findByDataTestId("Test-artifact-destination-input-wrapper");
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(testArtifact.destination()!);

        const newValue = "new-file.txt";
        testArtifact.destination(newValue);
        m.redraw.sync();

        expect(testArtifact.destination()).toEqual(newValue);
        expect(helper.findSelectorIn(inputWrapper, "input")).toHaveValue(newValue);
      });
    });

    it("should remove the artifact on cancel", () => {
      const closeIconForBuildArtifact = helper.findByDataTestId("Close-icon")[0];
      const closeIconForTestArtifact  = helper.findByDataTestId("Close-icon")[1];

      expect(artifacts.count()).toEqual(2);
      expect(helper.findByDataTestId("Build-artifact")).toBeInDOM();
      expect(helper.findByDataTestId("Test-artifact")).toBeInDOM();

      //remove build artifact
      simulateEvent.simulate(closeIconForBuildArtifact, "click");
      m.redraw.sync();

      expect(artifacts.count()).toEqual(1);
      expect(helper.findByDataTestId("Build-artifact")).not.toBeInDOM();
      expect(helper.findByDataTestId("Test-artifact")).toBeInDOM();

      //remove test artifact
      simulateEvent.simulate(closeIconForTestArtifact, "click");
      m.redraw.sync();

      expect(artifacts.count()).toEqual(0);
      expect(helper.findByDataTestId("Build-artifact")).not.toBeInDOM();
      expect(helper.findByDataTestId("Test-artifact")).not.toBeInDOM();
    });

    it("should add a new build artifact", () => {
      expect(artifacts.count()).toEqual(2);
      expect(helper.findByDataTestId("Build-artifact")).toHaveLength(1);
      expect(helper.findByDataTestId("Test-artifact")).toHaveLength(1);

      const selectDropdown: any = helper.findByDataTestId("form-field-input-artifact-type")[0];
      selectDropdown.value      = ArtifactType.Build;
      simulateEvent.simulate(selectDropdown, "change");
      m.redraw.sync();

      const addButton = helper.find("button");
      simulateEvent.simulate(addButton[0], "click");
      m.redraw.sync();

      expect(artifacts.count()).toEqual(3);
      expect(helper.findByDataTestId("Build-artifact")).toHaveLength(2);
      expect(helper.findByDataTestId("Test-artifact")).toHaveLength(1);
    });

    it("should add a new test artifact", () => {
      expect(artifacts.count()).toEqual(2);
      expect(helper.findByDataTestId("Build-artifact")).toHaveLength(1);
      expect(helper.findByDataTestId("Test-artifact")).toHaveLength(1);

      const selectDropdown: any = helper.findByDataTestId("form-field-input-artifact-type")[0];
      selectDropdown.value      = ArtifactType.Test;
      simulateEvent.simulate(selectDropdown, "change");
      m.redraw.sync();

      const addButton = helper.find("button");
      simulateEvent.simulate(addButton[0], "click");
      m.redraw.sync();

      expect(artifacts.count()).toEqual(3);
      expect(helper.findByDataTestId("Build-artifact")).toHaveLength(1);
      expect(helper.findByDataTestId("Test-artifact")).toHaveLength(2);
    });
  });
});
