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

import Stream from "mithril/stream";
import {Origin, OriginType} from "models/origin";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {PipelineConfigTestData} from "models/pipeline_configs/spec/test_data";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {ProjectManagementTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/project_management_tab_content";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("ProjectManagementTab", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should render pattern textfield", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);
    expect(pipelineConfig.trackingTool().regex()).toBeUndefined();
    expect(helper.byTestId("form-field-label-pattern")).toHaveText("Pattern");
    expect(helper.byTestId("project-management-pattern")).toHaveValue("");

    helper.oninput(helper.byTestId("project-management-pattern"), "##(\\d+)");

    expect(helper.byTestId("project-management-pattern")).toHaveValue("##(\\d+)");
    expect(pipelineConfig.trackingTool().regex()).toEqual("##(\\d+)");
  });

  it("should render url textfield", () => {
    const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
    mount(pipelineConfig);
    expect(pipelineConfig.trackingTool().urlPattern()).toBeUndefined();
    expect(helper.byTestId("form-field-label-uri")).toHaveText("URI");
    expect(helper.byTestId("project-management-uri")).toHaveValue("");

    helper.oninput(helper.byTestId("project-management-uri"), "https://github.com/gocd/gocd/issues/${ID}");

    expect(helper.byTestId("project-management-uri")).toHaveValue("https://github.com/gocd/gocd/issues/${ID}");
    expect(pipelineConfig.trackingTool().urlPattern()).toEqual("https://github.com/gocd/gocd/issues/${ID}");
  });

  describe("Read Only", () => {
    beforeEach(() => {
      const pipelineConfig = PipelineConfig.fromJSON(PipelineConfigTestData.withTwoStages());
      pipelineConfig.origin(new Origin(OriginType.ConfigRepo, "repo1"));
      mount(pipelineConfig);
    });

    it("should render disabled pattern", () => {
      expect(helper.byTestId("project-management-pattern")).toBeDisabled();
    });

    it("should render disabled uri", () => {
      expect(helper.byTestId("project-management-uri")).toBeDisabled();
    });
  });

  function mount(pipelineConfig: PipelineConfig, templateConfig = new TemplateConfig("foo", [])) {
    const routeParams = {} as PipelineConfigRouteParams;
    helper.mount(() => new ProjectManagementTabContent().content(pipelineConfig,
                                                                 templateConfig,
                                                                 routeParams,
                                                                 Stream<OperationState>(OperationState.UNKNOWN),
                                                                 new FlashMessageModelWithTimeout(),
                                                                 jasmine.createSpy(),
                                                                 jasmine.createSpy()));
  }
});
