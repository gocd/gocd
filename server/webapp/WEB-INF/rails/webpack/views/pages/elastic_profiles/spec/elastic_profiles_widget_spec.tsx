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
import {ElasticProfiles} from "models/elastic_profiles/types";
import {Extension} from "models/shared/plugin_infos_new/extensions";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import {TestData} from "views/pages/elastic_profiles/spec/test_data";
import {ElasticProfilesWidget} from "../elastic_profiles_widget";

describe("New Elastic Profiles Widget", () => {
  const simulateEvent = require("simulate-event");

  const pluginInfos = [
    PluginInfo.fromJSON(TestData.DockerPluginJSON(), TestData.DockerPluginJSON()._links),
    PluginInfo.fromJSON(TestData.DockerSwarmPluginJSON(), TestData.DockerSwarmPluginJSON()._links),
    PluginInfo.fromJSON(TestData.KubernatesPluginJSON(), TestData.KubernatesPluginJSON()._links)
  ];

  const elasticProfiles = ElasticProfiles.fromJSON([
                                                     TestData.DockerElasticProfile(),
                                                     TestData.DockerSwarmElasticProfile(),
                                                     TestData.K8SElasticProfile()
                                                   ]);

  let $root: any, root: any;
  beforeEach(() => {
    //@ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);

  //@ts-ignore
  afterEach(window.destroyDomElementForTest);

  describe("no elastic agent plugin loaded", () => {
    beforeEach(() => {
      mount([], elasticProfiles);
    });

    it("should list existing profiles in absence of elastic plugin", () => {
      expect(find("flash-message-info").text()).toEqual("No elastic agent plugin installed.");
      expect(find("key-value-value-plugin-id").eq(0)).toContainText(TestData.DockerPluginJSON().id);
      expect(find("key-value-value-plugin-id").eq(1)).toContainText(TestData.DockerSwarmPluginJSON().id);
      expect(find("key-value-value-plugin-id").eq(2)).toContainText(TestData.KubernatesPluginJSON().id);
    });

  });

  describe("list all profiles", () => {
    beforeEach(() => {
      mount(pluginInfos, elasticProfiles);
    });

    it("should render all elastic profile info panels", () => {
      expect(find("elastic-profile-list").get(0).children).toHaveLength(3);
    });

    it("should always render first profile info panel expanded", () => {
      expect(find("collapse-header").get(0)).toHaveClass(collapsiblePanelStyles.expanded);
      expect(find("collapse-header").get(1)).not.toHaveClass(collapsiblePanelStyles.expanded);
      expect(find("collapse-header").get(2)).not.toHaveClass(collapsiblePanelStyles.expanded);
    });

    it("should render plugin name, image and status report button", () => {
      expect(find("elastic-profile-list").get(0).children).toHaveLength(3);

      expect(find("plugin-name").get(0)).toContainText(TestData.DockerPluginJSON().about.name);
      expect(find("plugin-icon").get(0)).toHaveAttr("src", TestData.DockerPluginJSON()._links.image.href);
      expect(find("status-report-link").get(0)).toBeVisible();

      expect(find("plugin-name").get(1)).toContainText(TestData.DockerSwarmPluginJSON().about.name);
      expect(find("plugin-icon").get(1)).toHaveAttr("src", TestData.DockerSwarmPluginJSON()._links.image.href);
      expect(find("status-report-link").get(1)).toBeVisible();

      expect(find("plugin-name").get(2)).toContainText(TestData.KubernatesPluginJSON().about.name);
      expect(find("plugin-icon").get(2)).toHaveAttr("src", TestData.KubernatesPluginJSON()._links.image.href);
      expect(find("status-report-link").get(2)).toBeVisible();
    });

    it("should toggle between expanded and collapsed state on click of header", () => {
      const elasticProfileListHeader = find("collapse-header").get(1);

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);

      //expand elastic profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw();

      expect(elasticProfileListHeader).toHaveClass(collapsiblePanelStyles.expanded);

      //collapse elastic profile info
      simulateEvent.simulate(elasticProfileListHeader, "click");
      m.redraw();

      expect(elasticProfileListHeader).not.toHaveClass(collapsiblePanelStyles.expanded);
    });
  });

  describe("StatusReport", () => {
    it("should disable status report button when user is not an super admin", () => {
      mount(pluginInfos, elasticProfiles, false);
      expect(find("status-report-link").get(0)).toBeDisabled();
    });

    it("should not disable status report button when user is a super admin", () => {
      mount(pluginInfos, elasticProfiles, true);
      expect(find("status-report-link").get(0)).not.toBeDisabled();
    });
  });

  function mount(pluginInfos: Array<PluginInfo<Extension>>,
                 elasticProfiles: ElasticProfiles, isUserAnAdmin = true) {

    const noop = _.noop;

    m.mount(root, {
      view() {
        return (
          <ElasticProfilesWidget pluginInfos={stream(pluginInfos)}
                                 elasticProfiles={elasticProfiles}
                                 onEdit={noop}
                                 onClone={noop}
                                 onDelete={noop}
                                 onShowUsages={noop}
                                 isUserAnAdmin={isUserAnAdmin}/>
        );
      }
    });
    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
