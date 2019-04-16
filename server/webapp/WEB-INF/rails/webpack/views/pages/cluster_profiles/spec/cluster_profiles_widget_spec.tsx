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
import * as stream from "mithril/stream";
import {ClusterProfiles} from "models/cluster_profiles/cluster_profiles";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {ClusterProfilesWidget} from "views/pages/cluster_profiles/cluster_profiles_widget";
import {DockerClusterProfile, K8SClusterProfile} from "views/pages/cluster_profiles/spec/test_data";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";

describe("list all cluster profiles", () => {
  const helper        = new TestHelper();
  const simulateEvent = require("simulate-event");

  const pluginInfos = [
    PluginInfo.fromJSON(TestData.dockerPluginJSON(), TestData.dockerPluginJSON()._links),
    PluginInfo.fromJSON(TestData.kubernetesPluginJSON(), TestData.kubernetesPluginJSON()._links)
  ];

  const clusterProfiles = ClusterProfiles.fromJSON({
                                                     _embedded: {
                                                       cluster_profiles: [
                                                         DockerClusterProfile(),
                                                         K8SClusterProfile()
                                                       ]
                                                     }
                                                   });

  beforeEach(() => {
    mount(pluginInfos, clusterProfiles);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render all cluster profile panels", () => {
    expect(helper.findByDataTestId("cluster-profile-list").get(0).children).toHaveLength(2);
  });

  it("should render cluster id, plugin name and image", () => {
    expect(helper.findByDataTestId("cluster-profile-list").get(0).children).toHaveLength(2);

    expect(helper.findByDataTestId("plugin-name").get(0)).toContainText(TestData.dockerPluginJSON().about.name);
    expect(helper.findByDataTestId("plugin-icon").get(0))
      .toHaveAttr("src", TestData.dockerPluginJSON()._links.image.href);

    expect(helper.findByDataTestId("plugin-name").get(1)).toContainText(TestData.kubernetesPluginJSON().about.name);
    expect(helper.findByDataTestId("plugin-icon").get(1))
      .toHaveAttr("src", TestData.kubernetesPluginJSON()._links.image.href);
  });

  it("should toggle between expanded and collapsed state on click of header", () => {
    const clusterProfileListHeader = helper.findByDataTestId("collapse-header").get(1);

    expect(clusterProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand cluster profile info
    simulateEvent.simulate(clusterProfileListHeader, "click");
    m.redraw();

    expect(clusterProfileListHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse cluster profile info
    simulateEvent.simulate(clusterProfileListHeader, "click");
    m.redraw();

    expect(clusterProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  function mount(pluginInfos: Array<PluginInfo<Extension>>, clusterProfiles: ClusterProfiles) {
    helper.mount(() => <ClusterProfilesWidget pluginInfos={stream(pluginInfos)}
                                              clusterProfiles={clusterProfiles}/>);
  }
});
