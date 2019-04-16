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

import * as m from "mithril";
import {ClusterProfile} from "models/cluster_profiles/cluster_profiles";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as keyValuePairStyles from "views/components/key_value_pair/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {ClusterProfileWidget} from "views/pages/cluster_profiles/cluster_profiles_widget";
import {DockerClusterProfile} from "views/pages/cluster_profiles/spec/test_data";

describe("Cluster Profile Widget", () => {
  const simulateEvent  = require("simulate-event");
  const clusterProfile = ClusterProfile.fromJSON(DockerClusterProfile());
  const helper         = new TestHelper();

  beforeEach(() => {
    mount(clusterProfile);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render cluster profile id", () => {
    const profileHeader = helper.find(`.${keyValuePairStyles.keyValuePair}`).get(0);
    expect(profileHeader).toContainText("Cluster Profile Id");
    expect(profileHeader).toContainText(DockerClusterProfile().id);
  });

  it("should render properties of cluster profile", () => {
    const profileHeader = helper.find(`.${keyValuePairStyles.keyValuePair}`).get(1);

    expect(profileHeader).toContainText("go_server_url");
    expect(profileHeader).toContainText("https://localhost:8154/go");

    expect(profileHeader).toContainText("max_docker_containers");
    expect(profileHeader).toContainText("30");

    expect(profileHeader).toContainText("auto_register_timeout");
    expect(profileHeader).toContainText("10");

    expect(profileHeader).toContainText("docker_uri");
    expect(profileHeader).toContainText("unix:///var/docker.sock");
  });

  it("should toggle between expanded and collapsed state on click of header", () => {
    const clusterProfileHeader = find("collapse-header").get(0);

    expect(clusterProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand cluster profile info
    simulateEvent.simulate(clusterProfileHeader, "click");
    helper.redraw();

    expect(clusterProfileHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse cluster profile info
    simulateEvent.simulate(clusterProfileHeader, "click");
    helper.redraw();

    expect(clusterProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  function mount(clusterProfile: ClusterProfile) {
    helper.mount(() => <ClusterProfileWidget clusterProfile={clusterProfile}/>);
  }

  function find(id: string) {
    return helper.findByDataTestId(id);
  }
});
