/*
 * Copyright 2018 ThoughtWorks, Inc.
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
import {ElasticAgentProfiles} from "models/elastic_profiles/types";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";
import {ElasticProfilesWidget} from "../elastic_profiles_widget";

describe("Elastic Agent Profiles Widget", () => {
  const helper        = new TestHelper();
  const simulateEvent = require("simulate-event");

  const pluginInfos = [
    PluginInfo.fromJSON(TestData.dockerPluginJSON(), TestData.dockerPluginJSON()._links),
    PluginInfo.fromJSON(TestData.dockerSwarmPluginJSON(), TestData.dockerSwarmPluginJSON()._links),
    PluginInfo.fromJSON(TestData.kubernetesPluginJSON(), TestData.kubernetesPluginJSON()._links)
  ];

  const elasticProfiles = ElasticAgentProfiles.fromJSON([
                                                          TestData.dockerElasticProfile(),
                                                          TestData.dockerSwarmElasticProfile(),
                                                          TestData.kubernetesElasticProfile()
                                                        ]);

  describe("list all profiles", () => {
    beforeEach(() => {
      mount(pluginInfos, elasticProfiles);
    });

    afterEach(helper.unmount.bind(helper));

    it("should render all elastic agent profile info panels", () => {
      expect(helper.findByDataTestId("elastic-profile-list").get(0).children).toHaveLength(3);
    });

    it("should toggle between expanded and collapsed state on click of header", () => {
      const elasticProfileListHeader = helper.findByDataTestId("collapse-header").get(1);

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      //expand elastic agent profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw();

      expect(elasticProfileListHeader).toHaveClass(collapsiblePanelStyles.expanded);

      //collapse elastic agent profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw();

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    });
  });

  it("should display message to add new elastic agent profiles if no elastic agent profiles defined", () => {
    mount(pluginInfos, new ElasticAgentProfiles([]));

    expect(helper.findByDataTestId("flash-message-info")).toHaveText("Click on 'Add' button to create new elastic agent profile.");

    helper.unmount();
  });

  function mount(pluginInfos: Array<PluginInfo<Extension>>,
                 elasticProfiles: ElasticAgentProfiles, isUserAnAdmin = true) {

    const noop = _.noop;
    helper.mount(() => <ElasticProfilesWidget pluginInfos={stream(pluginInfos)}
                                              elasticProfiles={elasticProfiles}
                                              elasticAgentOperations={{
                                                onEdit: noop,
                                                onClone: noop,
                                                onDelete: noop,
                                                onAdd: noop
                                              }}
                                              onShowUsages={noop}
                                              isUserAnAdmin={isUserAnAdmin}/>);
  }
});
