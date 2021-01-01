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
import _ from "lodash";
import m from "mithril";
import {ElasticAgentProfiles} from "models/elastic_profiles/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {TestData} from "views/pages/elastic_agent_configurations/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ElasticProfilesWidget} from "../elastic_profiles_widget";

describe("Elastic Agent Profiles Widget", () => {
  const helper        = new TestHelper();

  const pluginInfo = PluginInfo.fromJSON(TestData.dockerPluginJSON());

  const elasticProfiles = ElasticAgentProfiles.fromJSON([
                                                          TestData.dockerElasticProfile(),
                                                          TestData.dockerSwarmElasticProfile(),
                                                          TestData.kubernetesElasticProfile()
                                                        ]);

  describe("list all profiles", () => {
    beforeEach(() => {
      mount(pluginInfo, elasticProfiles);
    });

    afterEach(helper.unmount.bind(helper));

    it("should render all elastic agent profile info panels", () => {
      expect(helper.byTestId("elastic-profile-list").children).toHaveLength(3);
    });

    it("should toggle between expanded and collapsed state on click of header", () => {
      const elasticProfileListHeader = helper.allByTestId("collapse-header").item(1);

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      //expand elastic agent profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw.sync();

      expect(elasticProfileListHeader).toHaveClass(collapsiblePanelStyles.expanded);

      //collapse elastic agent profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw.sync();

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    });
  });

  it("should display message to add new elastic agent profiles if no elastic agent profiles defined", () => {
    mount(pluginInfo, new ElasticAgentProfiles([]));

    expect(helper.textByTestId("flash-message-info")).toBe("Click on 'Add' button to create new elastic agent profile.");

    helper.unmount();
  });

  it("should not display message to add new elastic agent profiles if no elastic agent profiles defined", () => {
    mount(undefined, new ElasticAgentProfiles([]));

    expect(helper.byTestId("flash-message-info")).toBeFalsy();

    helper.unmount();
  });

  it("should disable action buttons if no elastic agent plugin installed", () => {
    mount(undefined, elasticProfiles, true);

    const elasticAgentProfilePanel = helper.byTestId("elastic-profile-header");

    expect(helper.byTestId("edit-elastic-profile", elasticAgentProfilePanel)).toBeDisabled();
    expect(helper.byTestId("clone-elastic-profile", elasticAgentProfilePanel)).toBeDisabled();
    expect(helper.byTestId("delete-elastic-profile", elasticAgentProfilePanel)).toBeDisabled();
    expect(helper.byTestId("show-usage-elastic-profile", elasticAgentProfilePanel)).not.toBeDisabled();

    helper.unmount();
  });

  it("should not disable action buttons if elastic agent plugin installed", () => {
    mount(pluginInfo, elasticProfiles, true);

    const elasticAgentProfilePanel = helper.byTestId("elastic-profile");

    expect(helper.byTestId("edit-elastic-profile", elasticAgentProfilePanel)).not.toBeDisabled();
    expect(helper.byTestId("clone-elastic-profile", elasticAgentProfilePanel)).not.toBeDisabled();
    expect(helper.byTestId("delete-elastic-profile", elasticAgentProfilePanel)).not.toBeDisabled();
    expect(helper.byTestId("show-usage-elastic-profile", elasticAgentProfilePanel)).not.toBeDisabled();

    helper.unmount();
  });

  function mount(pluginInfo: PluginInfo | undefined,
                 elasticProfiles: ElasticAgentProfiles, isUserAnAdmin = true) {

    const noop = _.noop;
    helper.mount(() => <ElasticProfilesWidget pluginInfo={pluginInfo}
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
