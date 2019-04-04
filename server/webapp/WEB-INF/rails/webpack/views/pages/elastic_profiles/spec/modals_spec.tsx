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

import {ClusterProfile, ClusterProfiles} from "models/elastic_profiles/types";
import {ExtensionType} from "models/shared/plugin_infos_new/extension_type";
import {ElasticAgentSettings} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";

import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {NewElasticProfileModal} from "views/pages/elastic_profiles/modals";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";

describe("New Elastic Profile Modals Spec", () => {
  let pluginInfo: PluginInfo<any>, clusterProfiles: ClusterProfile[] = [], helper: TestHelper;

  beforeEach(() => {
    clusterProfiles = [];
    pluginInfo     = PluginInfo.fromJSON(TestData.DockerPluginJSON(), TestData.DockerPluginJSON()._links);
    helper         = new TestHelper();
  });

  function mountModal() {
    const modal = new NewElasticProfileModal(
      [pluginInfo],
      new ClusterProfiles(clusterProfiles),
      () => {
        //do nothing
      });

    helper.mount(modal.body.bind(modal));
  }

  it("should get modal title", () => {
    const modal = new NewElasticProfileModal(
      [pluginInfo],
      new ClusterProfiles(clusterProfiles),
      () => {
        //do nothing
      });

    expect(modal.title()).toEqual("Add a new profile");
  });

  it("it should not render cluster profiles dropdown when plugin infos does not support cluster profiles", () => {
    mountModal();
    const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
    expect(!!(extension as ElasticAgentSettings).supportsClusterProfiles).toEqual(false);

    expect(find("form-field-label-cluster-profile-id")).not.toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).not.toBeInDOM();

    helper.unmount();
  });

  it("it should render an error message when plugin infos supports cluster profiles and no cluster profiles are defined",
     () => {
       const data                                   = TestData.DockerPluginJSON();
       data.extensions[0].supports_cluster_profiles = true;

       pluginInfo = PluginInfo.fromJSON(data, TestData.DockerPluginJSON()._links);

       mountModal();

       const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
       expect(!!(extension as ElasticAgentSettings).supportsClusterProfiles).toEqual(true);

       const expectedErrorMessage = "Can not create Elastic Agent Profile for plugin 'cd.go.contrib.elastic-agent.docker'. The plugin requires a Cluster Profile to be configured first in order to define an Elastic Agent Profile.";

       expect(find("flash-message-alert")).toContainText(expectedErrorMessage);
       expect(find("form-field-label-cluster-profile-id")).not.toBeInDOM();
       expect(find("form-field-input-cluster-profile-id")).not.toBeInDOM();

       helper.unmount();
     });

  it("should render cluster profiles dropdown", () => {
    const data = TestData.DockerPluginJSON();

    data.extensions[0].supports_cluster_profiles = true;

    pluginInfo     = PluginInfo.fromJSON(data, TestData.DockerPluginJSON()._links);
    clusterProfiles = [
      new ClusterProfile("cluster1", pluginInfo.id),
      new ClusterProfile("cluster2", pluginInfo.id),
    ];

    mountModal();

    const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
    expect(!!(extension as ElasticAgentSettings).supportsClusterProfiles).toEqual(true);

    expect(find("form-field-label-cluster-profile-id")).toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).toBeInDOM();

    expect(find("form-field-input-cluster-profile-id").get(0).children[0]).toContainText("cluster1");
    expect(find("form-field-input-cluster-profile-id").get(0).children[1]).toContainText("cluster2");

    helper.unmount();
  });

  it("should render cluster profiles belonging to the selected plugin", () => {
    const data = TestData.DockerPluginJSON();

    data.extensions[0].supports_cluster_profiles = true;

    pluginInfo     = PluginInfo.fromJSON(data, TestData.DockerPluginJSON()._links);
    clusterProfiles = [
      new ClusterProfile("cluster1", pluginInfo.id),
      new ClusterProfile("cluster3", "random.foo.plugin"),
      new ClusterProfile("cluster4", "another.random.foo.plugin"),
      new ClusterProfile("cluster2", pluginInfo.id)
    ];

    mountModal();

    const extension = pluginInfo.extensionOfType(ExtensionType.ELASTIC_AGENTS);
    expect(!!(extension as ElasticAgentSettings).supportsClusterProfiles).toEqual(true);

    expect(find("form-field-label-cluster-profile-id")).toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).toBeInDOM();

    expect(find("form-field-input-cluster-profile-id").get(0).children).toHaveLength(2);

    expect(find("form-field-input-cluster-profile-id").get(0).children[0]).toContainText("cluster1");
    expect(find("form-field-input-cluster-profile-id").get(0).children[1]).toContainText("cluster2");

    helper.unmount();
  });

  function find(id: string) {
    return helper.findByDataTestId(id);
  }
});
