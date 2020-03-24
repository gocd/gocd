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

import Stream from "mithril/stream";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/pipeline_config";
import {MaterialsTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/materials_tab_content";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("MaterialsTabContent", () => {
  const helper = new TestHelper();
  let materialTab: MaterialsTabContent;

  beforeEach(() => {
    materialTab = new MaterialsTabContent();
  });
  afterEach(helper.unmount.bind(helper));

  it("should render preconfigured materials", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
    mount(pipelineConfig);

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
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withGitMaterial());
    pipelineConfig.materials()[0].errors().add("name", "some error");
    pipelineConfig.materials()[0].errors().add("type", "some error on another property");
    mount(pipelineConfig);

    expect(helper.byTestId('flash-message-alert')).toBeInDOM();
    expect(helper.textByTestId('flash-message-alert')).toBe('some error.some error on another property.');
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

  function mount(pipelineConfig: PipelineConfig, templateConfig = new TemplateConfig("foo", [])) {
    const routeParams = {} as PipelineConfigRouteParams;
    helper.mount(() => materialTab.content(pipelineConfig,
                                           templateConfig,
                                           routeParams,
                                           Stream<OperationState>(OperationState.UNKNOWN)));
  }
});
