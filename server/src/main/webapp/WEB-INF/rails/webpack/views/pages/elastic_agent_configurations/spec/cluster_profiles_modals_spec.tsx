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

import {ClusterProfile} from "models/elastic_profiles/types";
import {Errors} from "models/mixins/errors";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ModalType} from "views/pages/elastic_agent_configurations/cluster_profiles_modals";
import {TestClusterProfile} from "views/pages/elastic_agent_configurations/spec/test_cluster_profiles_modal";
import {TestData} from "views/pages/elastic_agent_configurations/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ClusterProfileModal", () => {
  let pluginInfos: PluginInfos, helper: TestHelper, modal: TestClusterProfile;
  beforeEach(() => {
    pluginInfos = new PluginInfos(
      PluginInfo.fromJSON(TestData.dockerPluginJSON()),
      PluginInfo.fromJSON(TestData.kubernetesPluginJSON())
    );

    helper = new TestHelper();
  });

  describe("EditModalType", () => {
    it("id field should be disabled for edit modal type", () => {
      modal = new TestClusterProfile(pluginInfos, ModalType.edit, ClusterProfile.fromJSON(TestData.dockerClusterProfile()));
      helper.mount(modal.body.bind(modal));

      expect(find("form-field-input-id")).toBeDisabled();

      helper.unmount();
    });

    it("should display warning message if selected plugin do not support cluster profile", () => {
      const clusterProfile = new ClusterProfile("", "cd.go.contrib.elasticagent.kubernetes");
      modal                = new TestClusterProfile(pluginInfos, ModalType.edit, clusterProfile);
      helper.mount(modal.body.bind(modal));
      expect(helper.byTestId("flash-message-warning", find("cluster-profile-form-header"))).toBeInDOM();
      expect(helper.byTestId("flash-message-warning", find("cluster-profile-form-header"))).toHaveText("Can not edit Cluster profile for 'Kubernetes Elastic Agent Plugin' plugin as it does not support cluster profiles.");
      expect(helper.byTestId("cluster-profile-properties-form")).toBeEmpty();

      helper.unmount();
    });
  });

  describe("CreateModalType", () => {
    it("id field should not be disabled for new modal type", () => {
      const clusterProfile = new ClusterProfile("", "cd.go.contrib.elastic-agent.docker", true, new Configurations([]));
      modal                = new TestClusterProfile(pluginInfos, ModalType.create, clusterProfile);
      helper.mount(modal.body.bind(modal));

      expect(find("form-field-input-id")).toBeInDOM();
      expect(find("form-field-input-id")).not.toBeDisabled();

      helper.unmount();
    });

    it("should display cluster profile properties form if selected plugin supports cluster profile", () => {
      const clusterProfile = new ClusterProfile("", "cd.go.contrib.elastic-agent.docker", true, new Configurations([]));
      modal                = new TestClusterProfile(pluginInfos, ModalType.create, clusterProfile);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("flash-message-alert", find("cluster-profile-form-header"))).toBeFalsy();
      expect(helper.byTestId("cluster-profile-properties-form")).toBeTruthy();

      helper.unmount();
    });

    it("should display message if selected plugin do not support cluster profile", () => {
      const clusterProfile = new ClusterProfile("", "cd.go.contrib.elasticagent.kubernetes");
      modal                = new TestClusterProfile(pluginInfos, ModalType.create, clusterProfile);
      helper.mount(modal.body.bind(modal));

      expect(helper.byTestId("flash-message-alert", find("cluster-profile-form-header"))).toBeInDOM();
      expect(helper.byTestId("flash-message-alert", find("cluster-profile-form-header"))).toHaveText("Can not define Cluster profiles for 'Kubernetes Elastic Agent Plugin' plugin as it does not support cluster profiles.");
      expect(helper.byTestId("cluster-profile-properties-form")).toBeEmpty();

      helper.unmount();
    });
  });

  it("should have modal title and fields", () => {
    modal = new TestClusterProfile(pluginInfos, ModalType.create, ClusterProfile.fromJSON(TestData.dockerClusterProfile()));
    helper.mount(modal.body.bind(modal));

    expect(modal.title()).toEqual("Modal title");
    expect(find("form-field-label-id")).toBeInDOM();
    expect(find("form-field-label-plugin-id")).toBeInDOM();

    expect(find("form-field-input-plugin-id")).toBeInDOM();
    expect(find("form-field-input-plugin-id").children[0]).toContainText("Docker Elastic Agent Plugin");
    expect(find("form-field-input-plugin-id").children[1]).toContainText("Kubernetes Elastic Agent Plugin");

    helper.unmount();
  });

  it("should display error message if there is error on a field", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.errors(new Errors({id: ["should be unique"]}));
    modal = new TestClusterProfile(pluginInfos, ModalType.edit, clusterProfile);
    helper.mount(modal.body.bind(modal));

    expect(find("form-field-input-id").parentElement).toContainText("should be unique");

    helper.unmount();
  });

  it("should display error message", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    modal                = new TestClusterProfile(pluginInfos, ModalType.edit, clusterProfile);
    modal.setErrorMessageForTest("some error message");
    helper.mount(modal.body.bind(modal));

    expect(find("flash-message-alert")).toBeInDOM();
    expect(find("flash-message-alert")).toContainText("some error message");

    helper.unmount();
  });

  it("should display spinner if no cluster profile specified", () => {
    modal = new TestClusterProfile(pluginInfos, ModalType.edit);
    helper.mount(modal.body.bind(modal));

    expect(find("spinner-wrapper").parentElement).toBeInDOM();

    helper.unmount();
  });

  function find(id: string) {
    return helper.byTestId(id);
  }
});
