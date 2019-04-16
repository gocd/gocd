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

import * as _ from "lodash";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {ClusterProfile, ClusterProfiles, ElasticAgentProfile, ElasticAgentProfiles} from "models/elastic_profiles/types";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {ClusterProfilesWidget} from "views/pages/elastic_profiles/cluster_profiles_widget";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";

const simulateEvent = require("simulate-event");

describe("ClusterProfilesWidget", () => {
  const helper                                                 = new TestHelper();
  const kubernetesPluginJSON                                   = TestData.kubernetesPluginJSON();
  kubernetesPluginJSON.extensions[0].supports_cluster_profiles = true;
  kubernetesPluginJSON.extensions[0].cluster_profile_settings  = kubernetesPluginJSON.extensions[0].plugin_settings;

  const pluginInfos = [
    PluginInfo.fromJSON(TestData.dockerPluginJSON(), TestData.dockerPluginJSON()._links),
    PluginInfo.fromJSON(kubernetesPluginJSON, kubernetesPluginJSON._links)
  ];

  it("should render all cluster profiles", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findByDataTestId("cluster-profile-panel")).toHaveLength(2);

    helper.unmount();
  });

  it("should display the message in absence of elastic plugin", () => {
    mount([], new ClusterProfiles([]), new ElasticAgentProfiles([]));

    expect(helper.findByDataTestId("flash-message-info").text()).toEqual("No elastic agent plugin installed.");

    helper.unmount();
  });

  it("should display message to add cluster profiles if no cluster profiles defined", () => {
    mount(pluginInfos, new ClusterProfiles([]), new ElasticAgentProfiles([]));

    expect(helper.findByDataTestId("flash-message-info").text()).toEqual("Click on 'Add' button to create new cluster profile.");

    helper.unmount();
  });

  it("should display new elastic agent profile button", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findByDataTestId("new-elastic-agent-profile-button")).toBeInDOM();
    expect(helper.findByDataTestId("new-elastic-agent-profile-button")).toHaveText("+ New Elastic Agent Profile");

    helper.unmount();
  });

  it("should display cluster profile status report button", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findIn(helper.findByDataTestId("cluster-profile-panel")[0], "status-report-link")).toBeInDOM();
    expect(helper.findIn(helper.findByDataTestId("cluster-profile-panel")[1], "status-report-link")).not.toBeInDOM();

    helper.unmount();
  });

  it("should contain cluster profile id and plugin id in header", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findIn(helper.findByDataTestId("collapse-header")[0], "cluster-profile-name")).toHaveText("cluster_3");
    expect(helper.findIn(helper.findByDataTestId("collapse-header")[0], "key-value-key-pluginid")).toHaveText("PluginId");
    expect(helper.findIn(helper.findByDataTestId("collapse-header")[0], "key-value-value-pluginid")).toHaveText("cd.go.contrib.elastic-agent.docker");

    helper.unmount();
  });

  it("should contain plugin icon in header if specified in plugin info", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findIn(helper.findByDataTestId("collapse-header")[0], "plugin-icon")).toBeInDOM();

    helper.unmount();
  });

  it("should not contain plugin icon in header if not specified in plugin info", () => {
    const links = TestData.dockerPluginJSON()._links;
    delete links.image;
    const pluginInfos     = [PluginInfo.fromJSON(TestData.dockerPluginJSON(), links)];
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findIn(helper.findByDataTestId("collapse-header")[0], "plugin-icon")).not.toBeInDOM();

    helper.unmount();
  });

  it("should contain cluster profile information", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-id")).toHaveText("Id");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-id")).toHaveText("cluster_3");

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-pluginid")).toHaveText("PluginId");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-pluginid")).toHaveText("cd.go.contrib.elastic-agent.docker");

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-go-server-url")).toHaveText("go_server_url");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-go-server-url")).toHaveText("https://localhost:8154/go");

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-max-docker-containers")).toHaveText("max_docker_containers");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-max-docker-containers")).toHaveText("30");

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-auto-register-timeout")).toHaveText("auto_register_timeout");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-auto-register-timeout")).toHaveText("10");

    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-key-docker-uri")).toHaveText("docker_uri");
    expect(helper.findIn(helper.findByDataTestId("collapse-body")[0], "key-value-value-docker-uri")).toHaveText("unix:///var/docker.sock");

    helper.unmount();
  });

  it("should contain elastic agent profile details for respective cluster profile", () => {
    const dockerElasticProfile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    dockerElasticProfile.clusterProfileId("cluster_3");

    const kubernetesElasticProfile = ElasticAgentProfile.fromJSON(TestData.kubernetesElasticProfile());
    kubernetesElasticProfile.clusterProfileId("cluster_1");

    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([dockerElasticProfile, kubernetesElasticProfile]));

    const dockerElasticProfilePanel = helper.findByDataTestId("elastic-profile")[0];
    expect(helper.findIn(dockerElasticProfilePanel, "elastic-profile-id")).toHaveText("Profile2");
    expect(helper.findIn(dockerElasticProfilePanel, "key-value-value-image")).toHaveText("docker-image122345");
    expect(helper.findIn(dockerElasticProfilePanel, "key-value-value-command")).toHaveText("ls\n-alh");
    expect(helper.findIn(dockerElasticProfilePanel, "key-value-value-environment")).toHaveText("JAVA_HOME=/bin/java");
    expect(helper.findIn(dockerElasticProfilePanel, "key-value-value-hosts")).toHaveText("(Not specified)");

    const kubernetesElasticProfilePanel = helper.findByDataTestId("elastic-profile")[1];
    expect(helper.findIn(kubernetesElasticProfilePanel, "elastic-profile-id")).toHaveText("Kuber1");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-image")).toHaveText("Image1");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-maxmemory")).toHaveText("(Not specified)");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-maxcpu")).toHaveText("(Not specified)");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-environment")).toHaveText("(Not specified)");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-podconfiguration"))
      .toHaveText("apiVersion: v1\nkind: Pod\nmetadata:\n  name: pod-name-prefix-{{ POD_POSTFIX }}\n  labels:\n    app: web\nspec:\n  containers:\n    - name: gocd-agent-container-{{ CONTAINER_POSTFIX }}\n      image: {{ GOCD_AGENT_IMAGE }}:{{ LATEST_VERSION }}\n      securityContext:\n        privileged: true");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-specifiedusingpodconfiguration")).toHaveText("false");
    expect(helper.findIn(kubernetesElasticProfilePanel, "key-value-value-privileged")).toHaveText("(Not specified)");

    helper.unmount();
  });

  it("should contain action buttons for cluster profiles", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.findByDataTestId("cluster-profile-panel")[0];
    expect(helper.findIn(dockerClusterProfilePanel, "edit-cluster-profile")).toBeInDOM();
    expect(helper.findIn(dockerClusterProfilePanel, "delete-cluster-profile")).toBeInDOM();
    expect(helper.findIn(dockerClusterProfilePanel, "clone-cluster-profile")).toBeInDOM();

    helper.unmount();
  });

  it("should disable action buttons if no elastic agent plugin installed", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount([], clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.findByDataTestId("cluster-profile-panel")[0];
    expect(helper.findIn(dockerClusterProfilePanel, "edit-cluster-profile")).toBeDisabled();
    expect(helper.findIn(dockerClusterProfilePanel, "clone-cluster-profile")).toBeDisabled();
    expect(helper.findIn(dockerClusterProfilePanel, "new-elastic-agent-profile-button")).toBeDisabled();
    expect(helper.findIn(dockerClusterProfilePanel, "delete-cluster-profile")).not.toBeDisabled();

    helper.unmount();
  });

  describe("PanelsToggle", () => {
    let clusterProfilePanelHeader: any;

    beforeEach(() => {
      const clusterProfile      = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
      const elasticAgentProfile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
      elasticAgentProfile.clusterProfileId(clusterProfile.id());
      mount(pluginInfos, new ClusterProfiles([clusterProfile]), new ElasticAgentProfiles([elasticAgentProfile]));
      clusterProfilePanelHeader = helper.findIn(helper.findByDataTestId("cluster-profile-panel"), "collapse-header")[0];
    });

    afterEach(() => {
      helper.unmount();
    });

    it("should toggle expanded state of cluster profile on click", () => {
      expect(clusterProfilePanelHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      simulateEvent.simulate(clusterProfilePanelHeader, "click");

      expect(clusterProfilePanelHeader).toHaveClass(collapsiblePanelStyles.expanded);
    });

    it("should toggle expanded state of cluster profile show details on click", () => {
      simulateEvent.simulate(clusterProfilePanelHeader, "click");

      const clusterProfileInfoHeader = helper.findIn(helper.findByDataTestId("cluster-profile-info-panel")[0], "collapse-header")[0];

      expect(clusterProfileInfoHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      simulateEvent.simulate(clusterProfileInfoHeader, "click");

      expect(clusterProfileInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    });

    it("should toggle expanded state of elastic agent profile show details on click", () => {
      simulateEvent.simulate(clusterProfilePanelHeader, "click");

      const elasticAgentProfileInfoHeader = helper.findIn(helper.findByDataTestId("elastic-profile")[0], "collapse-header")[0];

      simulateEvent.simulate(elasticAgentProfileInfoHeader, "click");

      expect(elasticAgentProfileInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    });
  });

  function mount(pluginInfos: Array<PluginInfo<Extension>>, clusterProfiles: ClusterProfiles, elasticAgentProfiles: ElasticAgentProfiles) {
    const noop       = _.noop;
    const operations = {
      onEdit: noop,
      onClone: noop,
      onDelete: noop,
      onAdd: noop
    };
    helper.mount(() => <ClusterProfilesWidget pluginInfos={stream(pluginInfos)}
                                              clusterProfiles={clusterProfiles}
                                              elasticProfiles={elasticAgentProfiles}
                                              elasticAgentOperations={operations}
                                              clusterProfileOperations={operations}
                                              onShowUsages={noop}
                                              isUserAnAdmin={true}/>);
  }
});
