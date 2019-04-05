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

import {ClusterProfile} from "models/cluster_profiles/cluster_profiles";
import {Errors} from "models/mixins/errors";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {ModalType} from "views/pages/cluster_profiles/modals";
import {TestClusterProfile} from "views/pages/cluster_profiles/spec/test_cluster_profile_modal";
import {DockerClusterProfile, DockerPluginJSON} from "views/pages/cluster_profiles/spec/test_data";

describe("ClusterProfileModalSpecs", () => {
  let pluginInfo: PluginInfo<any>, helper: TestHelper, modal: TestClusterProfile;
  beforeEach(() => {
    pluginInfo = PluginInfo.fromJSON(DockerPluginJSON(), []);
    helper     = new TestHelper();
  });

  describe("EditModalType", () => {
    beforeEach(() => {
      modal = new TestClusterProfile([pluginInfo], ModalType.edit, ClusterProfile.fromJSON(DockerClusterProfile()));
      helper.mount(modal.body.bind(modal));
    });

    afterEach(() => {
      helper.unmount();
    });

    it("should have modal title and fields", () => {
      expect(modal.title()).toEqual("Modal title");
      expect(find("form-field-label-id")).toBeInDOM();
      expect(find("form-field-label-plugin-id")).toBeInDOM();
    });

    it("id field should be disabled for edit modal type", () => {
      expect(find("form-field-input-id")).toBeDisabled();
    });
  });

  describe("CreateModalType", () => {
    beforeEach(() => {
      modal = new TestClusterProfile([pluginInfo], ModalType.create, ClusterProfile.fromJSON(DockerClusterProfile()));
      helper.mount(modal.body.bind(modal));
    });

    afterEach(() => {
      helper.unmount();
    });

    it("id field should not be disabled for edit modal type", () => {
      expect(find("form-field-input-id")).not.toBeDisabled();
    });
  });

  it("should display error message", () => {
    const clusterProfile = ClusterProfile.fromJSON(DockerClusterProfile());
    clusterProfile.errors(new Errors({id: ["should be unique"]}));
    modal = new TestClusterProfile([pluginInfo], ModalType.edit, clusterProfile);
    helper.mount(modal.body.bind(modal));

    expect(find("form-field-input-id").parent()).toContainText("should be unique");

    helper.unmount();
  });

  it("should display error message", () => {
    const clusterProfile = ClusterProfile.fromJSON(DockerClusterProfile());
    modal                = new TestClusterProfile([pluginInfo], ModalType.edit, clusterProfile);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(find("flash-message-alert")).toBeInDOM();
    expect(find("flash-message-alert")).toContainText("some error message");

    helper.unmount();
  });

  function find(id: string) {
    return helper.findByDataTestId(id);
  }
});
