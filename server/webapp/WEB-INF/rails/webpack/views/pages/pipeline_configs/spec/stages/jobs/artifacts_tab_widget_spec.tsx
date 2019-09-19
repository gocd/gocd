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
    expect(helper.byTestId("artifacts-tab")).toBeInDOM();
  });

  it("should render artifacts heading", () => {
    expect(helper.textByTestId("artifacts-heading")).toContain("Artifacts");
  });

  it("should render artifacts help text", () => {
    const helpText = "Artifacts are the files created during a job run, by one of the tasks. Publish your artifacts so that it can be used by downstream pipelines.";
    expect(helper.text("#help-artifacts")).toContain(helpText);
  });

  it("should render doc link for artifact help text", () => {
    const docLink      = helper.q("#help-artifacts a");
    const expectedLink = "/configuration/dev_upload_test_report.html";
    expect((docLink as HTMLAnchorElement).getAttribute("href")).toContain(expectedLink);
  });

  describe("Built-in Artifact", () => {
    it("should render build artifact", () => {
      const buildArtifacts = helper.allByTestId("Build-artifact");
      expect(buildArtifacts).toHaveLength(1);
    });

    it("should render test artifact", () => {
      const testArtifacts = helper.allByTestId("Test-artifact");
      expect(testArtifacts).toHaveLength(1);
    });

    it("should render headings for built in artifact", () => {
      const headingContainers = helper.allByTestId("artifact-item-headings");
      expect(headingContainers).toHaveLength(2);

      expect(headingContainers).toContainText("Type");
      expect(headingContainers).toContainText("Source");
      expect(headingContainers).toContainText("Destination");
    });

    it("should render help text for type", () => {
      const helpText = "There are 3 types of artifacts - build, test and external. When 'Test Artifact' is selected, Go will use this artifact to generate a test report. Test information is placed in the Failures and Test sub-tabs. Test results from multiple jobs are aggregated on the stage detail pages. This allows you to see the results of tests from both functional and unit tests even if they are run in different jobs. When artifact type external is selected, you can configure the external artifact store to which you can push an artifact.";
      expect(helper.text("#help-artifact-type")).toContain(helpText);
    });

    it("should render help text for source", () => {
      const helpText = "The file or folders to publish to the server. Go will only upload files that are in the working directory of the job. You can use wildcards to specify the files and folders to upload (** means any path, * means any file or folder name).";
      expect(helper.text("#help-artifact-source")).toContain(helpText);
    });

    it("should render help text for destination", () => {
      const helpText = "The destination is relative to the artifacts folder of the current instance on the server side. If it is not specified, the artifact will be stored in the root of the artifacts directory";
      expect(helper.text("#help-artifact-destination")).toContain(helpText);
    });

    describe("Build", () => {
      it("should render type of build artifact", () => {
        expect(helper.text("span", helper.byTestId("Build-artifact-content"))).toContain("Build Artifact");
      });

      it("should render input field for build artifact source", () => {
        expect(helper.q("input", helper.byTestId("Build-artifact-source-input-wrapper"))).toBeInDOM();
      });

      it("should bind input field with model source field", () => {
        const input = helper.q("input", helper.byTestId("Build-artifact-source-input-wrapper"));
        expect(input).toHaveValue(buildArtifact.source()!);

        const newValue = "new-file.txt";
        buildArtifact.source(newValue);
        m.redraw.sync();

        expect(buildArtifact.source()).toBe(newValue);
        expect(input).toHaveValue(newValue);
      });

      it("should render input field for build artifact destination", () => {
        const input = helper.q("input", helper.byTestId("Build-artifact-destination-input-wrapper"));
        expect(input).toBeInDOM();

        expect(input).toHaveValue(buildArtifact.destination()!);
      });

      it("should bind input field with model destination field", () => {
        const input = helper.q("input", helper.byTestId("Build-artifact-destination-input-wrapper"));
        expect(input).toHaveValue(buildArtifact.destination()!);

        const newValue = "new-file.txt";
        buildArtifact.destination(newValue);
        m.redraw.sync();

        expect(buildArtifact.destination()).toBe(newValue);
        expect(input).toHaveValue(newValue);
      });
    });

    describe("Test", () => {
      it("should render type of test artifact", () => {
        expect(helper.text("span", helper.byTestId("Test-artifact-content"))).toContain("Test Artifact");
      });

      it("should render input field for test artifact source", () => {
        expect(helper.q("input", helper.byTestId("Test-artifact-source-input-wrapper"))).toBeInDOM();
      });

      it("should bind input field with model source field", () => {
        const input = helper.q("input", helper.byTestId("Test-artifact-source-input-wrapper"));
        expect(input).toHaveValue(testArtifact.source()!);

        const newValue = "new-file.txt";
        testArtifact.source(newValue);
        m.redraw.sync();

        expect(testArtifact.source()).toBe(newValue);
        expect(input).toHaveValue(newValue);
      });

      it("should render input field for test artifact destination", () => {
        const input = helper.q("input", helper.byTestId("Test-artifact-destination-input-wrapper"));
        expect(input).toBeInDOM();

        expect(input).toHaveValue(testArtifact.destination()!);
      });

      it("should bind input field with model destination field", () => {
        const input = helper.q("input", helper.byTestId("Test-artifact-destination-input-wrapper"));
        expect(input).toHaveValue(testArtifact.destination()!);

        const newValue = "new-file.txt";
        testArtifact.destination(newValue);
        m.redraw.sync();

        expect(testArtifact.destination()).toBe(newValue);
        expect(input).toHaveValue(newValue);
      });
    });

    it("should remove the artifact on cancel", () => {
      const closeIconForBuildArtifact = helper.allByTestId("Close-icon")[0];
      const closeIconForTestArtifact  = helper.allByTestId("Close-icon")[1];

      expect(artifacts.count()).toBe(2);
      expect(helper.byTestId("Build-artifact")).toBeInDOM();
      expect(helper.byTestId("Test-artifact")).toBeInDOM();

      //remove build artifact
      helper.click(closeIconForBuildArtifact);

      expect(artifacts.count()).toBe(1);
      expect(helper.byTestId("Build-artifact")).toBeFalsy();
      expect(helper.byTestId("Test-artifact")).toBeInDOM();

      //remove test artifact
      helper.click(closeIconForTestArtifact);

      expect(artifacts.count()).toBe(0);
      expect(helper.byTestId("Build-artifact")).toBeFalsy();
      expect(helper.byTestId("Test-artifact")).toBeFalsy();
    });

    it("should add a new build artifact", () => {
      expect(artifacts.count()).toBe(2);
      expect(helper.allByTestId("Build-artifact")).toHaveLength(1);
      expect(helper.allByTestId("Test-artifact")).toHaveLength(1);

      helper.onchange(helper.byTestId("form-field-input-artifact-type"), ArtifactType.Build);
      helper.click("button");

      expect(artifacts.count()).toBe(3);
      expect(helper.allByTestId("Build-artifact")).toHaveLength(2);
      expect(helper.allByTestId("Test-artifact")).toHaveLength(1);
    });

    it("should add a new test artifact", () => {
      expect(artifacts.count()).toBe(2);
      expect(helper.allByTestId("Build-artifact")).toHaveLength(1);
      expect(helper.allByTestId("Test-artifact")).toHaveLength(1);

      helper.onchange(helper.byTestId("form-field-input-artifact-type"), ArtifactType.Test);
      helper.click("button");

      expect(artifacts.count()).toBe(3);
      expect(helper.allByTestId("Build-artifact")).toHaveLength(1);
      expect(helper.allByTestId("Test-artifact")).toHaveLength(2);
    });
  });
});
