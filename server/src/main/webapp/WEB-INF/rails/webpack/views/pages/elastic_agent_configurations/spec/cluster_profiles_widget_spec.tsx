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
import m from "mithril";
import Stream from "mithril/stream";
import {
  ClusterProfile,
  ClusterProfiles,
  ElasticAgentProfile,
  ElasticAgentProfiles
} from "models/elastic_profiles/types";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {ClusterProfilesWidget} from "views/pages/elastic_agent_configurations/cluster_profiles_widget";
import elasticProfilePageStyles from "views/pages/elastic_agent_configurations/index.scss";
import {TestData} from "views/pages/elastic_agent_configurations/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ClusterProfilesWidget", () => {
  const helper                                                 = new TestHelper();
  const kubernetesPluginJSON                                   = TestData.kubernetesPluginJSON();
  kubernetesPluginJSON.extensions[0].supports_cluster_profiles = true;
  kubernetesPluginJSON.extensions[0].cluster_profile_settings  = kubernetesPluginJSON.extensions[0].plugin_settings;

  const pluginInfos = new PluginInfos(
    PluginInfo.fromJSON(TestData.dockerPluginJSON()),
    PluginInfo.fromJSON(kubernetesPluginJSON)
  );

  it("should render all cluster profiles", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.allByTestId("cluster-profile-panel")).toHaveLength(2);

    helper.unmount();
  });

  it("should display the message in absence of elastic plugin", () => {
    mount(new PluginInfos(), new ClusterProfiles([]), new ElasticAgentProfiles([]));

    expect(helper.textByTestId("flash-message-info")).toBe("No elastic agent plugin installed.");

    helper.unmount();
  });

  it("should display message to add cluster profiles if no cluster profiles defined", () => {
    mount(pluginInfos, new ClusterProfiles([]), new ElasticAgentProfiles([]));

    expect(helper.textByTestId("flash-message-info")).toBe("Click on 'Add' button to create new cluster profile.");

    helper.unmount();
  });

  it("should display new elastic agent profile button", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId("new-elastic-agent-profile-button")).toBeInDOM();
    expect(helper.byTestId("new-elastic-agent-profile-button")).toHaveText("Elastic Agent Profile");

    helper.unmount();
  });

  it("should display cluster profile status report button", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId( "status-report-link", helper.allByTestId("cluster-profile-panel").item(0))).toBeInDOM();
    expect(helper.byTestId( "status-report-link", helper.allByTestId("cluster-profile-panel").item(1))).toBeFalsy();

    helper.unmount();
  });

  it("should contain cluster profile id and plugin id in header", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId("cluster-profile-name", helper.byTestId("collapse-header"))).toHaveText("cluster_3");

    helper.unmount();
  });

  it("should contain plugin icon in header if specified in plugin info", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId("plugin-icon", helper.byTestId("collapse-header"))).toBeInDOM();

    helper.unmount();
  });

  it("should not contain plugin icon in header if not specified in plugin info", () => {
    const data            = TestData.dockerPluginJSON();
    delete data._links.image;
    const pluginInfos = new PluginInfos(PluginInfo.fromJSON(data));
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId("plugin-icon", helper.byTestId("collapse-header"))).not.toBeInDOM();

    helper.unmount();
  });

  it("should contain cluster profile information", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId("key-value-key-id", helper.byTestId("collapse-body"))).toHaveText("Id");
    expect(helper.byTestId("key-value-value-id", helper.byTestId("collapse-body"))).toHaveText("cluster_3");

    expect(helper.byTestId("key-value-key-pluginid", helper.byTestId("collapse-body"))).toHaveText("PluginId");
    expect(helper.byTestId("key-value-value-pluginid", helper.byTestId("collapse-body"))).toHaveText("cd.go.contrib.elastic-agent.docker");

    expect(helper.byTestId("key-value-key-go-server-url", helper.byTestId("collapse-body"))).toHaveText("go_server_url");
    expect(helper.byTestId("key-value-value-go-server-url", helper.byTestId("collapse-body"))).toHaveText("https://localhost:8154/go");

    expect(helper.byTestId("key-value-key-max-docker-containers", helper.byTestId("collapse-body"))).toHaveText("max_docker_containers");
    expect(helper.byTestId("key-value-value-max-docker-containers", helper.byTestId("collapse-body"))).toHaveText("30");

    expect(helper.byTestId("key-value-key-auto-register-timeout", helper.byTestId("collapse-body"))).toHaveText("auto_register_timeout");
    expect(helper.byTestId("key-value-value-auto-register-timeout", helper.byTestId("collapse-body"))).toHaveText("10");

    expect(helper.byTestId("key-value-key-docker-uri", helper.byTestId("collapse-body"))).toHaveText("docker_uri");
    expect(helper.byTestId("key-value-value-docker-uri", helper.byTestId("collapse-body"))).toHaveText("unix:///var/docker.sock");

    helper.unmount();
  });

  it("should contain elastic agent profile details for respective cluster profile", () => {
    const dockerElasticProfile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    dockerElasticProfile.clusterProfileId("cluster_3");

    const kubernetesElasticProfile = ElasticAgentProfile.fromJSON(TestData.kubernetesElasticProfile());
    kubernetesElasticProfile.clusterProfileId("cluster_1");

    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile()), ClusterProfile.fromJSON(TestData.kubernetesClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([dockerElasticProfile, kubernetesElasticProfile]));

    const dockerElasticProfilePanel = helper.byTestId("elastic-profile-header");
    expect(helper.byTestId("elastic-profile-id", dockerElasticProfilePanel)).toHaveText("Profile2");
    expect(helper.byTestId("key-value-value-image", dockerElasticProfilePanel)).toHaveText("docker-image122345");
    expect(helper.byTestId("key-value-value-command", dockerElasticProfilePanel)).toHaveText("ls\n-alh");
    expect(helper.byTestId("key-value-value-environment", dockerElasticProfilePanel)).toHaveText("JAVA_HOME=/bin/java");
    expect(helper.byTestId("key-value-value-hosts", dockerElasticProfilePanel)).toHaveText("(Not specified)");

    const kubernetesElasticProfilePanel = helper.allByTestId("elastic-profile-header").item(1);
    expect(helper.byTestId("elastic-profile-id", kubernetesElasticProfilePanel)).toHaveText("Kuber1");
    expect(helper.byTestId("key-value-value-image", kubernetesElasticProfilePanel)).toHaveText("Image1");
    expect(helper.byTestId("key-value-value-maxmemory", kubernetesElasticProfilePanel)).toHaveText("(Not specified)");
    expect(helper.byTestId("key-value-value-maxcpu", kubernetesElasticProfilePanel)).toHaveText("(Not specified)");
    expect(helper.byTestId("key-value-value-environment", kubernetesElasticProfilePanel)).toHaveText("(Not specified)");
    expect(helper.byTestId("key-value-value-podconfiguration", kubernetesElasticProfilePanel))
      .toHaveText("apiVersion: v1\nkind: Pod\nmetadata:\n  name: pod-name-prefix-{{ POD_POSTFIX }}\n  labels:\n    app: web\nspec:\n  containers:\n    - name: gocd-agent-container-{{ CONTAINER_POSTFIX }}\n      image: {{ GOCD_AGENT_IMAGE }}:{{ LATEST_VERSION }}\n      securityContext:\n        privileged: true");
    expect(helper.byTestId("key-value-value-specifiedusingpodconfiguration", kubernetesElasticProfilePanel)).toHaveText("false");
    expect(helper.byTestId("key-value-value-privileged", kubernetesElasticProfilePanel)).toHaveText("(Not specified)");

    helper.unmount();
  });

  it("should contain action buttons for cluster profiles", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("edit-cluster-profile", dockerClusterProfilePanel)).toBeInDOM();
    expect(helper.byTestId("delete-cluster-profile", dockerClusterProfilePanel)).toBeInDOM();
    expect(helper.byTestId("clone-cluster-profile", dockerClusterProfilePanel)).toBeInDOM();

    helper.unmount();
  });

  it("should disable action buttons if no elastic agent plugin installed", () => {
    const clusterProfiles = new ClusterProfiles([ClusterProfile.fromJSON(TestData.dockerClusterProfile())]);
    mount(new PluginInfos(), clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("edit-cluster-profile", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("clone-cluster-profile", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("new-elastic-agent-profile-button", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("delete-cluster-profile", dockerClusterProfilePanel)).toBeDisabled();

    helper.unmount();
  });

  it("should not disable status button when user does not have administer permissions", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.canAdminister(false);

    const clusterProfiles = new ClusterProfiles([clusterProfile]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    expect(helper.byTestId( "status-report-link", helper.allByTestId("cluster-profile-panel").item(0))).toBeInDOM();
    expect(helper.byTestId( "status-report-link", helper.allByTestId("cluster-profile-panel").item(0))).not.toBeDisabled();

    helper.unmount();
  });

  it("should disable new elastic agent profile button when user does not have administer permissions", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.canAdminister(false);

    const clusterProfiles = new ClusterProfiles([clusterProfile]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("new-elastic-agent-profile-button", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("new-elastic-agent-profile-button", dockerClusterProfilePanel).title).toBe("You dont have permissions to administer 'cluster_3' cluster profile.");

    helper.unmount();
  });

  it("should disable edit button when user does not have administer permissions", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.canAdminister(false);

    const clusterProfiles = new ClusterProfiles([clusterProfile]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("edit-cluster-profile", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("edit-cluster-profile", dockerClusterProfilePanel).title).toBe("You dont have permissions to administer 'cluster_3' cluster profile.");

    helper.unmount();
  });

  it("should not disable clone button when user does not have administer permissions", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.canAdminister(false);

    const clusterProfiles = new ClusterProfiles([clusterProfile]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("clone-cluster-profile", dockerClusterProfilePanel)).not.toBeDisabled();

    helper.unmount();
  });

  it("should disable delete button when user does not have administer permissions", () => {
    const clusterProfile = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
    clusterProfile.canAdminister(false);

    const clusterProfiles = new ClusterProfiles([clusterProfile]);
    mount(pluginInfos, clusterProfiles, new ElasticAgentProfiles([]));

    const dockerClusterProfilePanel = helper.byTestId("cluster-profile-panel");
    expect(helper.byTestId("delete-cluster-profile", dockerClusterProfilePanel)).toBeDisabled();
    expect(helper.byTestId("delete-cluster-profile", dockerClusterProfilePanel).title).toBe("You dont have permissions to administer 'cluster_3' cluster profile.");

    helper.unmount();
  });

  describe("PanelsToggle", () => {
    let clusterProfilePanelHeader: any;

    beforeEach(() => {
      const clusterProfile      = ClusterProfile.fromJSON(TestData.dockerClusterProfile());
      const elasticAgentProfile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
      elasticAgentProfile.clusterProfileId(clusterProfile.id());
      mount(pluginInfos, new ClusterProfiles([clusterProfile]), new ElasticAgentProfiles([elasticAgentProfile]));
      clusterProfilePanelHeader = helper.byTestId("collapse-header", helper.byTestId("cluster-profile-panel"));
    });

    afterEach(() => {
      helper.unmount();
    });

    it("should toggle expanded state of cluster profile on click", () => {
      expect(clusterProfilePanelHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      helper.click(clusterProfilePanelHeader);

      expect(clusterProfilePanelHeader).toHaveClass(collapsiblePanelStyles.expanded);
    });

    it("should toggle expanded state of cluster profile show details on click", () => {
      helper.click(clusterProfilePanelHeader);

      const clusterProfileInfoHeader = helper.byTestId("cluster-profile-details-header", helper.byTestId("cluster-profile-panel"));

      expect(clusterProfileInfoHeader).not.toHaveClass(elasticProfilePageStyles.expanded);
      expect(helper.byTestId("cluster-profile-details", helper.byTestId("cluster-profile-panel"))).not.toHaveClass(elasticProfilePageStyles.expanded);

      helper.click(clusterProfileInfoHeader);

      expect(clusterProfileInfoHeader).toHaveClass(elasticProfilePageStyles.expanded);
      expect(helper.byTestId("cluster-profile-details", helper.byTestId("cluster-profile-panel"))).toHaveClass(elasticProfilePageStyles.expanded);
    });

    it("should toggle expanded state of elastic agent profile show details on click", () => {
      helper.click(clusterProfilePanelHeader);

      const elasticAgentProfileInfoHeader = helper.byTestId("collapse-header", helper.byTestId("elastic-profile-header"));

      helper.click(elasticAgentProfileInfoHeader);

      expect(elasticAgentProfileInfoHeader).toHaveClass(collapsiblePanelStyles.expanded);
    });
  });

  function mount(pluginInfos: PluginInfos, clusterProfiles: ClusterProfiles, elasticAgentProfiles: ElasticAgentProfiles) {
    const noop       = _.noop;
    const operations = {
      onEdit: noop,
      onClone: noop,
      onDelete: noop,
      onAdd: noop
    };
    helper.mount(() => <ClusterProfilesWidget pluginInfos={Stream(pluginInfos)}
                                              clusterProfiles={clusterProfiles}
                                              elasticProfiles={elasticAgentProfiles}
                                              elasticAgentOperations={operations}
                                              clusterProfileOperations={operations}
                                              onShowUsages={noop}
                                              isUserAnAdmin={true}/>);
  }
});
