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

import m from "mithril";
import Stream from "mithril/stream";
import {ClusterProfile, ElasticAgentProfile} from "models/elastic_profiles/types";
import {Configurations} from "models/shared/configuration";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {pluginInfoWithElasticAgentExtensionV5} from "models/shared/plugin_infos_new/spec/test_data";
import {Wizard} from "views/components/wizard";
import {openWizard} from "views/pages/elastic_agents/wizard";

describe("ElasticAgentWizard", () => {
  let wizard: Wizard;
  let pluginInfos;
  let clusterProfile;
  let elasticProfile;
  let onSuccessfulSave: any;
  let onError;

  beforeEach(() => {
    pluginInfos      = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV5)));
    clusterProfile   = Stream(new ClusterProfile(undefined,
                                                 undefined,
                                                 new Configurations([])));
    elasticProfile   = Stream(new ElasticAgentProfile(undefined,
                                                      undefined,
                                                      undefined,
                                                      new Configurations([])));
    onSuccessfulSave = jasmine.createSpy();
    onError          = jasmine.createSpy();

    wizard = openWizard(pluginInfos, clusterProfile, elasticProfile, onSuccessfulSave, onError);
    m.redraw.sync();
  });

  afterEach(() => {
    wizard.close();
    m.redraw.sync();
  });

  it("should display cluster profile properties form", () => {
    expect(wizard).toContainElementWithDataTestId("form-field-input-cluster-profile-name");
    expect(wizard).toContainElementWithDataTestId("form-field-input-plugin-id");
    expect(wizard).toContainElementWithDataTestId("properties-form");
    expect(wizard).toContainInBody("elastic agent plugin settings view");
  });

  it("should display elastic profile properties form", () => {
    wizard.next();
    m.redraw.sync();
    expect(wizard).toContainElementWithDataTestId("form-field-input-elastic-profile-name");
    expect(wizard).toContainElementWithDataTestId("properties-form");
    expect(wizard).toContainInBody("some view for plugin");
  });
});
