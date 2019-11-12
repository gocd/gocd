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
import {ArtifactConfig} from "models/server-configuration/server_configuration";
import {TestHelper} from "views/pages/spec/test_helper";
import {ArtifactsManagementWidget} from "../artifacts_management_widget";

describe("ArtifactsManagementWidget", () => {
  const helper = new TestHelper();
  const onCancelSpy = jasmine.createSpy("onCancel");
  const onSaveSpy = jasmine.createSpy("onSave");
  afterEach((done) => helper.unmount(done));

  it("should render text input field for artifact directory", () => {
    mount(new ArtifactConfig("foo"));
    expect(helper.byTestId("form-field-input-artifacts-directory-location")).toBeInDOM();
    expect(helper.byTestId("form-field-input-artifacts-directory-location")).toHaveValue("foo");
    expect(helper.byTestId("form-field-label-artifacts-directory-location"))
      .toContainText("Artifacts Directory Location");
  });

  it("should render checkbox for cleanup artifact settings", () => {
    const artifactConfig = new ArtifactConfig("foo", 10, 20);
    mount(artifactConfig);

    expect(helper.byTestId("form-field-input-allow-auto-cleanup-artifacts")).toBeInDOM();
    expect(helper.byTestId("form-field-label-allow-auto-cleanup-artifacts")).toBeInDOM();
    expect(helper.byTestId("form-field-input-allow-auto-cleanup-artifacts")).toHaveProp("checked", true);
    expect(helper.byTestId("form-field-label-allow-auto-cleanup-artifacts"))
      .toContainText("Allow auto cleanup artifacts");

  });

  it("should render purgeStartDiskSpace and purgeUptoDiskSpace input fields for cleanup artifacts settings", () => {
    const artifactConfig = new ArtifactConfig("foo", 10, 20);
    mount(artifactConfig);

    expect(helper.byTestId("purge-start-disk-space")).toBeInDOM();
    expect(helper.byTestId("purge-upto-disk-space")).toBeInDOM();
    expect(helper.byTestId("purge-start-disk-space")).not.toBeDisabled();
    expect(helper.byTestId("purge-upto-disk-space")).not.toBeDisabled();
    expect(helper.byTestId("form-field-label-trigger-when-disk-space-is")).toContainText("Trigger when disk space is");
    expect(helper.byTestId("purge-start-disk-space")).toHaveValue("10");
    expect(helper.byTestId("form-field-label-target-disk-space")).toContainText("Target disk space");
    expect(helper.byTestId("purge-upto-disk-space")).toHaveValue("20");

  });

  it("should enable purgeStartDiskSpace and purgeUptoDiskSpace input fields - when cleanup artifact is selected",
    () => {
      const artifactConfig = new ArtifactConfig("foo");
      mount(artifactConfig);
      expect(helper.byTestId("form-field-input-allow-auto-cleanup-artifacts")).toHaveProp("checked", false);

      expect(helper.byTestId("purge-start-disk-space")).toBeDisabled();
      expect(helper.byTestId("purge-upto-disk-space")).toBeDisabled();
      helper.click(helper.byTestId("form-field-input-allow-auto-cleanup-artifacts"));

      expect(helper.byTestId("purge-start-disk-space")).not.toBeDisabled();
      expect(helper.byTestId("purge-upto-disk-space")).not.toBeDisabled();
    });

  describe("Cancel", () => {
    it("should render cancel button", () => {
      mount(new ArtifactConfig("foo"));
      expect(helper.byTestId("cancel")).toHaveText("Cancel");
    });

    it("should call onCancel", () => {
      const artifactConfig = new ArtifactConfig("foo");
      mount(artifactConfig);
      helper.oninput(helper.byTestId("form-field-input-artifacts-directory-location"), "foobar");
      helper.clickByTestId("cancel");
      expect(onCancelSpy).toHaveBeenCalled();
    });
  });

  describe("Save", () => {
    it("should have save button", () => {
      mount(new ArtifactConfig("foo"));
      expect(helper.byTestId("save")).toBeInDOM();
    });

    it("should call onSave", () => {
      mount(new ArtifactConfig("foo"));
      helper.oninput(helper.byTestId("form-field-input-artifacts-directory-location"), "foobar");
      helper.click(helper.byTestId("save"));
      expect(onSaveSpy).toHaveBeenCalled();
    });

  });

  function mount(artifactConfig: ArtifactConfig) {
    const savePromise   = new Promise((resolve) => {
      onSaveSpy();
      resolve();
    });
    const cancelPromise = new Promise((resolve) => {
      onCancelSpy();
      resolve();
    });
    helper.mount(() => <ArtifactsManagementWidget artifactConfig={artifactConfig} onCancel={() => cancelPromise}
                                                  onArtifactConfigSave={() => savePromise}/>);
  }
});
