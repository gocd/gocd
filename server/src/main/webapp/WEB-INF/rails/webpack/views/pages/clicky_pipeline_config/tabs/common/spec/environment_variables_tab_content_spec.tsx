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
import {EnvironmentVariable, EnvironmentVariables} from "models/environment_variables/types";
import {Origin, OriginType} from "models/origin";
import {PipelineConfig} from "models/pipeline_configs/pipeline_config";
import {TemplateConfig} from "models/pipeline_configs/template_config";
import {FlashMessageModelWithTimeout} from "views/components/flash_message";
import {PipelineConfigRouteParams} from "views/pages/clicky_pipeline_config/tab_handler";
import {PipelineEnvironmentVariablesTabContent} from "views/pages/clicky_pipeline_config/tabs/pipeline/pipeline_environment_variable_tab_content";
import {OperationState} from "views/pages/page_operations";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environment Variables Tab Content", () => {
  const helper = new TestHelper();
  let tab: PipelineEnvironmentVariablesTabContent;

  beforeEach(() => {
    document.body.setAttribute("data-meta", JSON.stringify({pipelineName: "pipeline1"}));
    tab = new PipelineEnvironmentVariablesTabContent();
  });

  afterEach(helper.unmount.bind(helper));

  it("should render environment variables", () => {
    const pipeline = new PipelineConfig();
    pipeline.origin(new Origin(OriginType.GoCD, undefined));
    const envVar = new EnvironmentVariable("key", "value");
    pipeline.environmentVariables(new EnvironmentVariables(envVar));
    mount(pipeline);

    expect(helper.byTestId("environment-variables")).toBeInDOM();

    expect(helper.byTestId("env-var-name")).toHaveValue("key");
    expect(helper.byTestId("env-var-value")).toHaveValue("value");
    expect(helper.byTestId("env-var-name")).not.toBeDisabled();
    expect(helper.byTestId("env-var-value")).not.toBeDisabled();
    expect(helper.byTestId("remove-env-var-btn")).toBeInDOM();
  });

  it("should render readonly environment variables", () => {
    const pipeline = new PipelineConfig();
    pipeline.origin(new Origin(OriginType.ConfigRepo, "id1"));
    const envVar = new EnvironmentVariable("key", "value");
    pipeline.environmentVariables(new EnvironmentVariables(envVar));
    mount(pipeline);

    expect(helper.byTestId("readonly-environment-variables")).toBeInDOM();

    expect(helper.byTestId("env-var-name")).toHaveValue("key");
    expect(helper.byTestId("env-var-value")).toHaveValue("value");
    expect(helper.byTestId("env-var-name")).toBeDisabled();
    expect(helper.byTestId("env-var-value")).toBeDisabled();
    expect(helper.byTestId("remove-env-var-btn")).not.toBeInDOM();
  });

  function mount(entity: PipelineConfig | TemplateConfig) {
    helper.mount(() => {
      const template    = new TemplateConfig("template", []);
      const routeParams = {} as PipelineConfigRouteParams;
      return tab.content(entity, template, routeParams, Stream<OperationState>(OperationState.UNKNOWN),
                         new FlashMessageModelWithTimeout(), jasmine.createSpy(), jasmine.createSpy());
    });
  }
});
