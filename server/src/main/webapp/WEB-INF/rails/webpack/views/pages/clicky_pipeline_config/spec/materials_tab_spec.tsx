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

import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {MaterialsTab} from "views/pages/clicky_pipeline_config/materials_tab";
import {TestHelper} from "views/pages/spec/test_helper";

describe('MaterialsTab', () => {
  const helper = new TestHelper();
  let materialTab: MaterialsTab;

  beforeEach(() => {
    materialTab = new MaterialsTab();
  });
  afterEach(helper.unmount.bind(helper));

  it('should render preconfigured materials', () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
    mount(pipelineConfig);

    const headerRow = helper.byTestId("table-header-row");
    expect(helper.qa("th", headerRow)[0]).toHaveText("Type");
    expect(helper.qa("th", headerRow)[1]).toHaveText("Material Name");
    expect(helper.qa("th", headerRow)[2]).toHaveText("Url");

    const dataRow = helper.byTestId("table-row");
    expect(helper.qa("td", dataRow)).toHaveLength(4);
    expect(helper.qa("td", dataRow)[0]).toHaveText("git");
    expect(helper.qa("td", dataRow)[1]).toHaveText("GM");
    expect(helper.qa("td", dataRow)[2]).toHaveText("test-repo");
  });

  describe("Add Material", () => {
    it("should render button", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);

      expect(helper.byTestId("add-material-button")).toBeInDOM();
    });

    it("should call addNewMaterial method on click of the button", () => {
      const addNewMaterialSpyFunction = spyOn(materialTab, "addNewMaterial");
      const pipelineConfig            = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);

      helper.clickByTestId("add-material-button");

      expect(addNewMaterialSpyFunction).toHaveBeenCalled();
    });
  });

  describe("Edit Material", () => {
    it("should render button", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);

      expect(helper.byTestId("edit-material-button")).toBeInDOM();
    });

    it("should call updateMaterial method on click of the button", () => {
      const updateMaterialSpyFunction = spyOn(materialTab, "updateMaterial");
      const pipelineConfig            = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);

      helper.clickByTestId("edit-material-button");

      expect(updateMaterialSpyFunction).toHaveBeenCalled();
    });
  });

  describe("Delete Material", () => {
    it("should render button", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);

      expect(helper.byTestId("delete-material-button")).toBeInDOM();
    });

    it("should delete material on click of button", () => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
      mount(pipelineConfig);
      expect(pipelineConfig.materials()).toHaveLength(1);

      helper.clickByTestId("delete-material-button");

      expect(pipelineConfig.materials()).toHaveLength(0);
    });
  });

  function mount(pipelineConfig: PipelineConfig) {
    helper.mount(() => materialTab.renderer(pipelineConfig));
  }
});
