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
import {ElasticAgentProfile} from "models/elastic_profiles/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as keyValuePairStyles from "views/components/key_value_pair/index.scss";
import {TestData} from "views/pages/elastic_agent_configurations/spec/test_data";
import {TestHelper} from "views/pages/spec/test_helper";
import {ElasticProfileWidget} from "../elastic_profiles_widget";

describe("New Elastic Agent Profile Widget", () => {

  const pluginInfo     = PluginInfo.fromJSON(TestData.dockerPluginJSON());
  const elasticProfile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
  const helper         = new TestHelper();

  it("should render elastic agent profile id", () => {
    mount(pluginInfo, elasticProfile);

    expect(helper.textByTestId("elastic-profile-id")).toContain(TestData.dockerElasticProfile().id);

    helper.unmount();
  });

  it("should render edit, delete, clone buttons", () => {
    mount(pluginInfo, elasticProfile);

    expect(find("edit-elastic-profile")).toBeVisible();
    expect(find("clone-elastic-profile")).toBeVisible();
    expect(find("delete-elastic-profile")).toBeVisible();

    helper.unmount();
  });

  it("should render properties of elastic agent profile", () => {
    mount(pluginInfo, elasticProfile);

    const profileHeader = helper.q(`.${keyValuePairStyles.keyValuePair}`);

    expect(profileHeader).toContainText("Image");
    expect(profileHeader).toContainText("docker-image122345");

    expect(profileHeader).toContainText("Command");
    expect(profileHeader).toContainText("ls\n-alh");

    expect(profileHeader).toContainText("Environment");
    expect(profileHeader).toContainText("JAVA_HOME=/bin/java");

    expect(profileHeader).toContainText("Hosts");
    expect(profileHeader).toContainText("(Not specified)");

    helper.unmount();
  });

  it("should toggle between expanded and collapsed state on click of header", () => {
    mount(pluginInfo, elasticProfile);
    const elasticProfileHeader = find("collapse-header");

    expect(elasticProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand elastic agent profile info
    simulateEvent.simulate(elasticProfileHeader, "click");
    helper.redraw();

    expect(elasticProfileHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse elastic agent profile info
    simulateEvent.simulate(elasticProfileHeader, "click");
    helper.redraw();

    expect(elasticProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    helper.unmount();
  });

  it("should disable action buttons if no elastic agent plugin installed", () => {
    mount(undefined, ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile()));

    expect(helper.byTestId("edit-elastic-profile", helper.byTestId("elastic-profile-header"))).toBeDisabled();
    expect(helper.byTestId("clone-elastic-profile", helper.byTestId("elastic-profile-header"))).toBeDisabled();
    expect(helper.byTestId("delete-elastic-profile", helper.byTestId("elastic-profile-header"))).toBeDisabled();
    expect(helper.byTestId("show-usage-elastic-profile", helper.byTestId("elastic-profile-header"))).not.toBeDisabled();

    helper.unmount();
  });

  it("should disable edit button when user does not have administer permissions", () => {
    const profile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    profile.canAdminister(false);
    mount(pluginInfo, profile);

    expect(helper.byTestId("edit-elastic-profile", helper.byTestId("elastic-profile-header"))).toBeDisabled();
    expect(helper.byTestId("edit-elastic-profile", helper.byTestId("elastic-profile-header")).title).toBe("You dont have permissions to administer 'Profile2' elastic agent profile.");

    helper.unmount();
  });

  it("should not disable clone button when user does not have administer permissions", () => {
    const profile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    profile.canAdminister(false);
    mount(pluginInfo, profile);

    expect(helper.byTestId("clone-elastic-profile", helper.byTestId("elastic-profile-header"))).not.toBeDisabled();

    helper.unmount();
  });

  it("should disable delete button when user does not have administer permissions", () => {
    const profile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    profile.canAdminister(false);
    mount(pluginInfo, profile);

    expect(helper.byTestId("delete-elastic-profile", helper.byTestId("elastic-profile-header"))).toBeDisabled();
    expect(helper.byTestId("delete-elastic-profile", helper.byTestId("elastic-profile-header")).title).toBe("You dont have permissions to administer 'Profile2' elastic agent profile.");

    helper.unmount();
  });

  it("should not disable usage button when user does not have administer permissions", () => {
    const profile = ElasticAgentProfile.fromJSON(TestData.dockerElasticProfile());
    profile.canAdminister(false);
    mount(pluginInfo, profile);

    expect(helper.byTestId("show-usage-elastic-profile", helper.byTestId("elastic-profile-header"))).not.toBeDisabled();

    helper.unmount();
  });

  function mount(pluginInfo: PluginInfo | undefined, elasticProfile: ElasticAgentProfile) {
    const noop = _.noop;
    helper.mount(() => <ElasticProfileWidget pluginInfo={pluginInfo}
                                             elasticProfile={elasticProfile}
                                             onEdit={noop}
                                             onClone={noop}
                                             onDelete={noop}
                                             onShowUsage={noop}/>);
  }

  function find(id: string) {
    return helper.byTestId(id);
  }
});
