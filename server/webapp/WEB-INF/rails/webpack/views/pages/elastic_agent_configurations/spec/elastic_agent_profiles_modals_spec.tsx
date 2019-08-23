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

import _ from "lodash";
import { ClusterProfile, ClusterProfiles, ElasticAgentProfile } from "models/elastic_profiles/types";
import { Configurations } from "models/shared/configuration";
import {ExtensionTypeString} from "models/shared/plugin_infos_new/extension_type";
import { ElasticAgentExtension } from "models/shared/plugin_infos_new/extensions";
import { PluginInfo, PluginInfos } from "models/shared/plugin_infos_new/plugin_info";
import { NewElasticProfileModal } from "views/pages/elastic_agent_configurations/elastic_agent_profiles_modals";
import { TestData } from "views/pages/elastic_agent_configurations/spec/test_data";
import { TestHelper } from "views/pages/spec/test_helper";

describe("New Elastic Agent Profile Modals Spec", () => {
  let pluginInfo: PluginInfo, clusterProfiles: ClusterProfile[] = [], helper: TestHelper;

  beforeEach(() => {
    clusterProfiles = [];
    pluginInfo = PluginInfo.fromJSON(TestData.dockerPluginJSON());
    helper = new TestHelper();
  });

  function mountModal() {
    const modal = new NewElasticProfileModal(
      new PluginInfos(pluginInfo),
      new ClusterProfiles(clusterProfiles),
      new ElasticAgentProfile(undefined, undefined, undefined, new Configurations([])), _.noop);

    helper.mount(modal.body.bind(modal));
  }

  it("should get modal title", () => {
    const modal = new NewElasticProfileModal(
      new PluginInfos(pluginInfo),
      new ClusterProfiles(clusterProfiles),
      new ElasticAgentProfile(undefined, undefined, undefined, new Configurations([])), _.noop);

    expect(modal.title()).toEqual("Add a new elastic agent profile");
  });

  it("it should render an error message when plugin infos supports cluster profiles and no cluster profiles are defined", () => {
    mountModal();

    const extension = pluginInfo.extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS)!;
    expect(extension.supportsClusterProfiles).toEqual(true);

    const expectedErrorMessage = "Can not create Elastic Agent Profile for plugin 'cd.go.contrib.elastic-agent.docker'. A Cluster Profile must be configured first in order to define a new Elastic Agent Profile.";

    expect(find("flash-message-alert")).toContainText(expectedErrorMessage);

    expect(find("form-field-label-cluster-profile-id")).toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).toBeInDOM();

    helper.unmount();
  });

  it("should render cluster profiles dropdown", () => {
    clusterProfiles = [
      new ClusterProfile("cluster1", TestData.dockerPluginJSON().id),
      new ClusterProfile("cluster2", TestData.dockerPluginJSON().id),
    ];

    mountModal();

    const extension = pluginInfo.extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS)!;
    expect(extension.supportsClusterProfiles).toEqual(true);

    expect(find("form-field-label-cluster-profile-id")).toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).toBeInDOM();

    expect(find("form-field-input-cluster-profile-id").get(0).children[0]).toContainText("cluster1");
    expect(find("form-field-input-cluster-profile-id").get(0).children[1]).toContainText("cluster2");

    helper.unmount();
  });

  it("should render cluster profiles belonging to the selected plugin", () => {
    clusterProfiles = [
      new ClusterProfile("cluster1", pluginInfo.id),
      new ClusterProfile("cluster3", "random.foo.plugin"),
      new ClusterProfile("cluster4", "another.random.foo.plugin"),
      new ClusterProfile("cluster2", pluginInfo.id)
    ];

    mountModal();

    const extension = pluginInfo.extensionOfType<ElasticAgentExtension>(ExtensionTypeString.ELASTIC_AGENTS)!;
    expect(extension.supportsClusterProfiles).toEqual(true);

    expect(find("form-field-label-cluster-profile-id")).toBeInDOM();
    expect(find("form-field-input-cluster-profile-id")).toBeInDOM();

    expect(find("form-field-input-cluster-profile-id").get(0).children).toHaveLength(2);

    expect(find("form-field-input-cluster-profile-id").get(0).children[0]).toContainText("cluster1");
    expect(find("form-field-input-cluster-profile-id").get(0).children[1]).toContainText("cluster2");

    helper.unmount();
  });

  it("should show referenced cluster profile and plugin selected", () => {
    const data = TestData.kubernetesPluginJSON();

    data.extensions[0].supports_cluster_profiles = true;

    const pluginInfos = new PluginInfos(PluginInfo.fromJSON(TestData.dockerPluginJSON()), PluginInfo.fromJSON(data));
    clusterProfiles = [new ClusterProfile("cluster1", pluginInfo.id), new ClusterProfile("cluster2", data.id)];

    const elasticAgentProfile = new ElasticAgentProfile("", data.id, "cluster2", new Configurations([]));

    const modal = new NewElasticProfileModal(pluginInfos, new ClusterProfiles(clusterProfiles), elasticAgentProfile, _.noop);

    helper.mount(modal.body.bind(modal));

    expect(find("form-field-input-cluster-profile-id").get(0)).toHaveValue("cluster2");
    expect(find("form-field-input-cluster-profile-id").get(0)).toContainText(`cluster2 (${pluginInfos[1].about.name})`);

    helper.unmount();
  });

  it("should show first cluster profile selected if no cluster profile and plugin referenced", () => {
    const data = TestData.kubernetesPluginJSON();

    data.extensions[0].supports_cluster_profiles = true;

    const pluginInfos = new PluginInfos(PluginInfo.fromJSON(TestData.dockerPluginJSON()), PluginInfo.fromJSON(data));
    clusterProfiles = [new ClusterProfile("cluster1", pluginInfo.id), new ClusterProfile("cluster2", pluginInfo.id)];

    const elasticAgentProfile = new ElasticAgentProfile("", "", "", new Configurations([]));

    const modal = new NewElasticProfileModal(pluginInfos, new ClusterProfiles(clusterProfiles), elasticAgentProfile, _.noop);

    helper.mount(modal.body.bind(modal));

    expect(find("form-field-input-cluster-profile-id").get(0)).toHaveValue("cluster1");
    expect(find("form-field-input-cluster-profile-id").get(0)).toContainText(`cluster1 (${pluginInfos[0].about.name})`);

    helper.unmount();
  });

  function find(id: string) {
    return helper.findByDataTestId(id);
  }
});
