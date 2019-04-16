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
import {ElasticAgentProfile} from "models/elastic_profiles/types";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";

import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as keyValuePairStyles from "views/components/key_value_pair/index.scss";
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";

import {ElasticProfileWidget} from "../elastic_profiles_widget";

describe("New Elastic Profile Widget", () => {
  const simulateEvent  = require("simulate-event");
  const pluginInfo     = PluginInfo.fromJSON(TestData.DockerPluginJSON(), TestData.DockerPluginJSON()._links);
  const elasticProfile = ElasticAgentProfile.fromJSON(TestData.DockerElasticProfile());
  const helper         = new TestHelper();

  beforeEach(() => {
    mount(pluginInfo, elasticProfile);
  });
  afterEach(helper.unmount.bind(helper));

  it("should render elastic profile id", () => {
    expect(helper.findByDataTestId("elastic-profile-id").get(0)).toContainText("Profile Id");
    expect(helper.findByDataTestId("elastic-profile-id").get(0)).toContainText(TestData.DockerElasticProfile().id);
  });

  it("should render edit, delete, clone buttons", () => {
    expect(find("edit-elastic-profile")).toBeVisible();
    expect(find("clone-elastic-profile")).toBeVisible();
    expect(find("delete-elastic-profile")).toBeVisible();
  });

  it("should render properties of elastic profile", () => {
    const profileHeader = helper.find(`.${keyValuePairStyles.keyValuePair}`).get(0);

    expect(profileHeader).toContainText("Image");
    expect(profileHeader).toContainText("docker-image122345");

    expect(profileHeader).toContainText("Command");
    expect(profileHeader).toContainText("ls\n-alh");

    expect(profileHeader).toContainText("Environment");
    expect(profileHeader).toContainText("JAVA_HOME=/bin/java");

    expect(profileHeader).toContainText("Hosts");
    expect(profileHeader).toContainText("(Not specified)");
  });

  it("should toggle between expanded and collapsed state on click of header", () => {
    const elasticProfileHeader = find("collapse-header").get(0);

    expect(elasticProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

    //expand elastic profile info
    simulateEvent.simulate(elasticProfileHeader, "click");
    helper.redraw();

    expect(elasticProfileHeader).toHaveClass(collapsiblePanelStyles.expanded);

    //collapse elastic profile info
    simulateEvent.simulate(elasticProfileHeader, "click");
    helper.redraw();

    expect(elasticProfileHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
  });

  function mount(pluginInfo: PluginInfo<Extension>,
                 elasticProfile: ElasticAgentProfile) {
    const noop = _.noop;
    helper.mount(() => <ElasticProfileWidget pluginInfo={pluginInfo}
                                             elasticProfile={elasticProfile}
                                             onEdit={noop}
                                             onClone={noop}
                                             onDelete={noop}
                                             onShowUsage={noop}/>);
  }

  function find(id: string) {
    return helper.findByDataTestId(id);
  }
});
