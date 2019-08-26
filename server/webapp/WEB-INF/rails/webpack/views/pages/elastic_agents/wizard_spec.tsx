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
import {pipelineStructure as pipelineStructureFixture} from "models/shared/pipeline_structure/pipeline_structure_spec";
import {PluginInfo, PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {pluginInfoWithElasticAgentExtensionV5} from "models/shared/plugin_infos_new/spec/test_data";
import {Wizard} from "views/components/wizard";
import {openWizard} from "views/pages/elastic_agents/wizard";

describe("ElasticAgentWizard", () => {
  let wizard: Wizard;
  let pluginInfos;
  let clusterProfile;
  let elasticProfile;
  let pipelineStructure;

  beforeEach(() => {
    pluginInfos       = Stream(new PluginInfos(PluginInfo.fromJSON(pluginInfoWithElasticAgentExtensionV5)));
    clusterProfile    = Stream(new ClusterProfile(undefined,
                                                  undefined,
                                                  new Configurations([])));
    elasticProfile    = Stream(new ElasticAgentProfile(undefined,
                                                       undefined,
                                                       undefined,
                                                       new Configurations([])));
    pipelineStructure = Stream(pipelineStructureFixture());

    wizard = openWizard(pluginInfos,
                        clusterProfile,
                        elasticProfile,
                        pipelineStructure);
    m.redraw.sync();
  });

  afterEach(() => {
    wizard.close();
    m.redraw.sync();
  });

  it("should display cluster profile properties form", () => {
    expect(wizard).toContainElementWithDataTestId("form-field-input-id");
    expect(wizard).toContainElementWithDataTestId("form-field-input-plugin-id");
    expect(wizard).toContainElementWithDataTestId("properties-form");
    expect(wizard).toContainInBody("elastic agent plugin settings view");
  });

  it("should display elastic profile properties form", () => {
    wizard.next();
    m.redraw.sync();
    expect(wizard).toContainElementWithDataTestId("form-field-input-id");
    expect(wizard).toContainElementWithDataTestId("properties-form");
    expect(wizard).toContainInBody("some view for plugin");
  });

  it("should display pipeline structure", () => {
    wizard.next(2);
    m.redraw.sync();
    expect(wizard).toContainElementWithDataTestId("form-field-label-g1");
    expect(wizard).toContainElementWithDataTestId("form-field-label-g1p1");
    expect(wizard).toContainElementWithDataTestId("form-field-label-s1");
    expect(wizard).toContainElementWithDataTestId("form-field-input-j1");
    expect(wizard).toContainElementWithDataTestId("form-field-input-template-1");
    expect(wizard).toContainInBody("You can't associate agent to pipeline - g1p2 as specified in config repo");
  });
});
