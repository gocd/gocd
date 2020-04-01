/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {Scms} from "models/materials/pluggable_scm";
import {PackageRepositories, Packages} from "models/package_repositories/package_repositories";
import {Materials, PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {MaterialsWidget} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe('MaterialsWidgetSpec', () => {
  const helper = new TestHelper();
  let pipelineConfig: PipelineConfig;

  beforeEach(() => {
    pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
  });
  afterEach((done) => helper.unmount(done));

  function mount(materials: Stream<Materials> = pipelineConfig.materials, pluginInfos: PluginInfos = new PluginInfos()) {
    helper.mount(() => <MaterialsWidget materials={materials} pluginInfos={Stream(pluginInfos)}
                                        packageRepositories={Stream(new PackageRepositories())}
                                        packages={Stream(new Packages())}
                                        onMaterialAdd={jasmine.createSpy("onMaterialAdd")}
                                        scmMaterials={Stream(new Scms())}/>);
  }

  describe("Add Material", () => {
    it("should render button", () => {
      mount();

      expect(helper.byTestId("add-material-button")).toBeInDOM();
    });
  });

  it("should render preconfigured materials", () => {
    mount();

    expect(helper.byTestId('flash-message-alert')).not.toBeInDOM();
    const headerRow = helper.byTestId("table-header-row");
    expect(helper.qa("th", headerRow)[0].textContent).toBe("Material Name");
    expect(helper.qa("th", headerRow)[1].textContent).toBe("Type");
    expect(helper.qa("th", headerRow)[2].textContent).toBe("Url");

    const dataRow = helper.byTestId("table-row");
    expect(helper.qa("td", dataRow)).toHaveLength(4);
    expect(helper.qa("td", dataRow)[0].textContent).toBe("GM");
    expect(helper.qa("td", dataRow)[1].textContent).toBe("Git");
    expect(helper.qa("td", dataRow)[2].textContent).toBe("test-repo");
  });

  it('should render all the errors on all the materials', () => {
    pipelineConfig.materials()[0].errors().add("name", "some error");
    pipelineConfig.materials()[0].errors().add("type", "some error on another property");
    mount();

    expect(helper.byTestId('flash-message-alert')).toBeInDOM();
    expect(helper.textByTestId('flash-message-alert')).toBe('some error.some error on another property.');
  });
});
